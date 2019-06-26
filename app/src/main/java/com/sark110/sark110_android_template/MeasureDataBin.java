package com.sark110.sark110_android_template;

/**
 * This file is a part of the "SARK110 Antenna Vector Impedance Analyzer" software
 *
 * MIT License
 *
 * @author Copyright (c) 2018 Melchor Varela - EA4FRB
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

public class MeasureDataBin {
	private long mId;
	private int mFreq;
	private ComplexNumber mZs;

	public void set_RefImp(float r0) {
		this.m_z0 = new ComplexNumber(r0, 0);
	}

	private ComplexNumber m_z0 = new ComplexNumber(50.0, 0);

	public MeasureDataBin(){}

	public MeasureDataBin(long id, int freq, float Rs, float Xs){
		this.mId = id;
		this.mFreq = freq;
		this.mZs = new ComplexNumber(Rs, Xs);
	}

	public long getId(){
		return mId;
	}

	public void setId(long _id){
		mId = _id;
	}

	public int getFreq(){
		return mFreq;
	}
	public void setFreq(int _freq){
		mFreq =_freq;
	}

	public void setZs(float Rs, float Xs) {
		mZs = new ComplexNumber(Rs, Xs);
	}

	public ComplexNumber getZs() {
		return mZs;
	}
	public float getRs() {
		return (float) mZs.getRe();
	}
	public float getXs() {
		return (float) mZs.getIm();
	}
	public float getZsMag() {
		return (float) mZs.mod();
	}
	public float getZsAngle() { return (float)((mZs.getArg()/Math.PI) * 180.0);}
	public float getVswr(){
		return Z2Vswr (mZs, m_z0);
	}
	public float getRL() {
		ComplexNumber cxRho = Z2Rho(mZs, m_z0);
		if (cxRho.mod() == 0)
			return -99.999f;
		else
			return (float)(20f * Math.log10(cxRho.mod()));
	}
	public float getCL() {
		ComplexNumber cxRho = Z2Rho(mZs, m_z0);
		if (cxRho.mod() == 0)
			return 99.999f;
		else
			return (float)(Math.abs(20f / (2f * Math.log10(cxRho.mod()))));
	}
	public float getRhMag() {
		ComplexNumber cxRho = Z2Rho(mZs, m_z0);
		return (float)cxRho.mod();
	}
	public float getRhAngle() {
		ComplexNumber cxRho = Z2Rho(mZs, m_z0);
		return (float)((cxRho.getArg()/Math.PI) * 180.0);
	}
	public float getRefPwr() {
		ComplexNumber cxRho = Z2Rho(mZs, m_z0);
		return (float)(cxRho.mod() * cxRho.mod() * 100.0);
	}
	public float getQ() {
		if (mZs.getRe() == 0)
			return 999.99f;
		return (float)(Math.abs(mZs.getIm()/ mZs.getRe()));
	}
	public float getCs() { return (float) calcC(mZs, mFreq); }
	public float getLs() { return (float) calcL(mZs, mFreq); }

	/* Conversion functions */
	private ComplexNumber Z2Rho(ComplexNumber cxZ, ComplexNumber cxZ0)
	{
		return ComplexNumber.divide(ComplexNumber.subtract(cxZ, cxZ0), ComplexNumber.add(cxZ, cxZ0));
	}

	private float Z2Vswr (ComplexNumber cxZ, ComplexNumber cxZ0)
	{
		ComplexNumber cxRho = Z2Rho(cxZ, cxZ0);
		if (cxRho.mod() > 0.980197824)
			return 99.999f;
		return (1.0f + (float)cxRho.mod()) / (1.0f - (float)cxRho.mod());
	}

	private double calcL(ComplexNumber cxZ, int freq)
	{
		return cxZ.getIm() / (2.0 * Math.PI * ((double)freq / 1000000.0));
	}

	private double calcC(ComplexNumber cxZ, int freq)
	{
		if (cxZ.getIm() == 0)
			return -99999.99;
		return -1000000.0 / (cxZ.getIm() * 2.0 * Math.PI * ((double)freq / 1000000.0));
	}
}
