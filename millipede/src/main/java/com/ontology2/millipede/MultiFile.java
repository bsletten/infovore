package com.ontology2.millipede;

import static java.lang.Math.ceil;
import static java.lang.Math.log10;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.zip.GZIPOutputStream;

import com.google.common.io.Files;
import com.ontology2.millipede.sink.Sink;
import com.ontology2.millipede.source.CodecSource;
import com.ontology2.millipede.source.Source;

abstract public class MultiFile<T> implements MultiSource<T> {

	protected final String directory;
	protected final String nameBase;
	protected final String nameExtension;
	protected final PartitionFunction<T> f;

	public MultiFile(String directory, String nameBase, String nameExtension,
			PartitionFunction<T> f) {
		super();
		this.directory = directory;
		this.nameBase = nameBase;
		this.nameExtension = nameExtension;
		this.f = f;
	}

	public int getPartitionCount() {
		return getPartitionFunction().getPartitionCount();
	}
	
	public PartitionFunction<T> getPartitionFunction() {
		return f;
	}

	public String getFileName(int binNumber) {
		testBinNumber(binNumber);
		
		int length=(int) ceil(log10(f.getPartitionCount()));
		return String.format(
		    "%s/%s%0"+length+"d%s",
		    directory,
		    nameBase,
		    binNumber,
		    nameExtension
	    );
	}

	public boolean testExists() {
		String testFile=getFileName(0);
		return new File(testFile).exists();
	}

	private void testBinNumber(int binNumber) {
		if (binNumber<0 || binNumber>=f.getPartitionCount())
			throw new IllegalArgumentException("Bin Number ["+binNumber+"] is out of range");
	}

	abstract public void pushBin(int binNumber,Sink<T> destination) throws Exception;
	
	abstract public Sink<T> createSink(int binNumber) throws Exception;

	protected OutputStream createOutputStream(int binNumber) throws Exception {
		String fileName = getFileName(binNumber);
		Files.createParentDirs(new File(fileName));
		
		OutputStream stream=new FileOutputStream(fileName);
		if(fileName.endsWith(".gz")) {
			stream=new GZIPOutputStream(stream);
		}
		return stream;
	}

	protected PrintWriter createWriter(int binNumber) throws Exception {
		final OutputStream stream=createOutputStream(binNumber);
		OutputStreamWriter osw = new OutputStreamWriter(stream, "UTF-8");
		return new PrintWriter(osw) {
	        public void close() {
	            super.close();
	            try {
	                stream.close();
	            } catch (IOException e) {
	                e.printStackTrace();
	            }
	        }
	
	
	        public void flush() {
	            super.flush();
	            try {
	                stream.flush();
	            } catch (IOException e) {
	                e.printStackTrace();
	            }
	        }			
		};
	}

	protected InputStream createInputStream(int binNumber) throws Exception {
		String fileName = getFileName(binNumber);
		Files.createParentDirs(new File(fileName));
		return (new FileOpener()).createInputStream(fileName);
	}

	protected BufferedReader createReader(int binNumber) throws Exception {
		InputStream stream=createInputStream(binNumber);
		return new BufferedReader(
				new InputStreamReader(stream,"UTF-8")
				);
	}
}
