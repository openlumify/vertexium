package com.google.protobuf;

import org.apache.hadoop.io.Text;
import org.vertexium.accumulo.iterator.model.VertexiumAccumuloIteratorException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.List;

public class TextByteString extends ByteString {
    private final Text text;

    public TextByteString(Text text) {
        this.text = text;
    }

    @Override
    public byte byteAt(int i) {
        return text.getBytes()[i];
    }

    @Override
    byte internalByteAt(int i) {
        return text.getBytes()[i];
    }

    @Override
    public int size() {
        return text.getLength();
    }

    @Override
    public ByteString substring(int i, int i1) {
        throw new VertexiumAccumuloIteratorException("not implemented");
    }

    @Override
    protected void copyToInternal(byte[] bytes, int i, int i1, int i2) {
        throw new VertexiumAccumuloIteratorException("not implemented");
    }

    @Override
    public void copyTo(ByteBuffer byteBuffer) {
        throw new VertexiumAccumuloIteratorException("not implemented");
    }

    @Override
    public void writeTo(OutputStream outputStream) throws IOException {
        throw new VertexiumAccumuloIteratorException("not implemented");
    }

    @Override
    void writeToInternal(OutputStream outputStream, int i, int i1) throws IOException {
        throw new VertexiumAccumuloIteratorException("not implemented");
    }

    @Override
    void writeTo(ByteOutput byteOutput) throws IOException {
        byteOutput.write(text.getBytes(), 0, text.getLength());
    }

    @Override
    void writeToReverse(ByteOutput byteOutput) throws IOException {
        throw new VertexiumAccumuloIteratorException("not implemented");
    }

    @Override
    public ByteBuffer asReadOnlyByteBuffer() {
        throw new VertexiumAccumuloIteratorException("not implemented");
    }

    @Override
    public List<ByteBuffer> asReadOnlyByteBufferList() {
        throw new VertexiumAccumuloIteratorException("not implemented");
    }

    @Override
    protected String toStringInternal(Charset charset) {
        throw new VertexiumAccumuloIteratorException("not implemented");
    }

    @Override
    public boolean isValidUtf8() {
        throw new VertexiumAccumuloIteratorException("not implemented");
    }

    @Override
    protected int partialIsValidUtf8(int i, int i1, int i2) {
        throw new VertexiumAccumuloIteratorException("not implemented");
    }

    @Override
    public boolean equals(Object o) {
        throw new VertexiumAccumuloIteratorException("not implemented");
    }

    @Override
    public InputStream newInput() {
        throw new VertexiumAccumuloIteratorException("not implemented");
    }

    @Override
    public CodedInputStream newCodedInput() {
        throw new VertexiumAccumuloIteratorException("not implemented");
    }

    @Override
    protected int getTreeDepth() {
        throw new VertexiumAccumuloIteratorException("not implemented");
    }

    @Override
    protected boolean isBalanced() {
        throw new VertexiumAccumuloIteratorException("not implemented");
    }

    @Override
    protected int partialHash(int i, int i1, int i2) {
        throw new VertexiumAccumuloIteratorException("not implemented");
    }
}