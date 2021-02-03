package com.barikoi.barikoitrace.Utils;

import java.io.Serializable;
import java.net.NetworkInterface;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Date;
import java.util.Enumeration;
import java.util.concurrent.atomic.AtomicInteger;


public final class UniqueID implements Comparable<UniqueID>, Serializable {


    private static final int f192e;


    private static final short f193f;


    private static final AtomicInteger f194g = new AtomicInteger(new SecureRandom().nextInt());


    private static final char[] f195h = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};


    private final int f196a;


    private final int f197b;


    private final short f198c;


    private final int f199d;

    static {
        try {
            f192e = m392c();
            f193f = m394d();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public UniqueID() {
        this(new Date());
    }

    private UniqueID(int i, int i2, short s, int i3, boolean z) {
        if ((i2 & -16777216) != 0) {
            throw new IllegalArgumentException("The machine identifier must be between 0 and 16777215 (it must fit in three bytes).");
        } else if (!z || (i3 & -16777216) == 0) {
            this.f196a = i;
            this.f197b = i2;
            this.f198c = s;
            this.f199d = 16777215 & i3;
        } else {
            throw new IllegalArgumentException("The counter must be between 0 and 16777215 (it must fit in three bytes).");
        }
    }

    public UniqueID(Date date) {
        this(getTimeInSeconds(date), f192e, f193f, f194g.getAndIncrement(), false);
    }


    private static byte getByte(int i) {
        return (byte) i;
    }


    private static byte getByte(short s) {
        return (byte) s;
    }


    private static int getTimeInSeconds(Date date) {
        return (int) (date.getTime() / 1000);
    }


    private static byte m389b(int i) {
        return (byte) (i >> 8);
    }


    private static byte m390b(short s) {
        return (byte) (s >> 8);
    }


    private static byte m391c(int i) {
        return (byte) (i >> 16);
    }


    private static int m392c() {
        int nextInt;
        try {
            StringBuilder sb = new StringBuilder();
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface nextElement = networkInterfaces.nextElement();
                sb.append(nextElement.toString());
                byte[] hardwareAddress = nextElement.getHardwareAddress();
                if (hardwareAddress != null) {
                    ByteBuffer wrap = ByteBuffer.wrap(hardwareAddress);
                    try {
                        sb.append(wrap.getChar());
                        sb.append(wrap.getChar());
                        sb.append(wrap.getChar());
                    } catch (BufferUnderflowException e) {
                    }
                }
            }
            nextInt = sb.toString().hashCode();
        } catch (Throwable th) {
            nextInt = new SecureRandom().nextInt();
        }
        return nextInt & 16777215;
    }


    private static byte m393d(int i) {
        return (byte) (i >> 24);
    }


    private static short m394d() {
        return 0;
    }


    public static UniqueID createUniqueID() {
        return new UniqueID();
    }


    public int compareTo(UniqueID fVar) {
        if (fVar != null) {
            byte[] a = m398a();
            byte[] a2 = fVar.m398a();
            for (int i = 0; i < 12; i++) {
                if (a[i] != a2[i]) {
                    return (a[i] & 255) < (a2[i] & 255) ? -1 : 1;
                }
            }
            return 0;
        }
        throw null;
    }


    public void m397a(ByteBuffer byteBuffer) {
        BufferError.m357a("buffer", byteBuffer);
        BufferError.m358a("buffer.remaining() >=12", byteBuffer.remaining() >= 12);
        byteBuffer.put(m393d(this.f196a));
        byteBuffer.put(m391c(this.f196a));
        byteBuffer.put(m389b(this.f196a));
        byteBuffer.put(getByte(this.f196a));
        byteBuffer.put(m391c(this.f197b));
        byteBuffer.put(m389b(this.f197b));
        byteBuffer.put(getByte(this.f197b));
        byteBuffer.put(m390b(this.f198c));
        byteBuffer.put(getByte(this.f198c));
        byteBuffer.put(m391c(this.f199d));
        byteBuffer.put(m389b(this.f199d));
        byteBuffer.put(getByte(this.f199d));
    }


    public byte[] m398a() {
        ByteBuffer allocate = ByteBuffer.allocate(12);
        m397a(allocate);
        return allocate.array();
    }


    public String m399b() {
        char[] cArr = new char[24];
        byte[] a = m398a();
        int length = a.length;
        int i = 0;
        int i2 = 0;
        while (i < length) {
            byte b = a[i];
            int i3 = i2 + 1;
            char[] cArr2 = f195h;
            cArr[i2] = cArr2[(b >> 4) & 15];
            cArr[i3] = cArr2[b & 15];
            i++;
            i2 = i3 + 1;
        }
        return new String(cArr);
    }

    @Override // java.lang.Object
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || UniqueID.class != obj.getClass()) {
            return false;
        }
        UniqueID fVar = (UniqueID) obj;
        if (this.f199d != fVar.f199d) {
            return false;
        }
        if (this.f197b != fVar.f197b) {
            return false;
        }
        if (this.f198c != fVar.f198c) {
            return false;
        }
        return this.f196a == fVar.f196a;
    }

    @Override // java.lang.Object
    public int hashCode() {
        return (((((this.f196a * 31) + this.f197b) * 31) + this.f198c) * 31) + this.f199d;
    }

    @Override // java.lang.Object
    public String toString() {
        return m399b();
    }
}
