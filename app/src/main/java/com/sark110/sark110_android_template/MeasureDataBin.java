package com.sark110.sark110_android_template;

/**
 ******************************************************************************
 * @author  Melchor Varela - EA4FRB
 * @brief   SARK-110 interface for Android
 ******************************************************************************
 * @copy
 *
 *  This file is a part of the "SARK110 Antenna Vector Impedance Analyzer" software
 *
 *  "SARK110 Antenna Vector Impedance Analyzer software" is free software: you can redistribute it
 *  and/or modify it under the terms of the GNU General Public License as
 *  published by the Free Software Foundation, either version 3 of the License,
 *  or (at your option) any later version.
 *
 *  "SARK110 Antenna Vector Impedance Analyzer" software is distributed in the hope that it will be
 *  useful,  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with "SARK110 Antenna Vector Impedance Analyzer" software.  If not,
 *  see <http://www.gnu.org/licenses/>.
 *
 * <h2><center>&copy; COPYRIGHT 2018 Melchor Varela - EA4FRB </center></h2>
 *  Melchor Varela, Madrid, Spain.
 *  melchor.varela@gmail.com
 */

public class MeasureDataBin {
	private long mId;
	private float mFreq;
	private ComplexNumber mZs;

	public void set_RefImp(float r0) {
		this.m_z0 = new ComplexNumber(r0, 0);;
	}

	private ComplexNumber m_z0 = new ComplexNumber(50.0, 0);

	public MeasureDataBin(){}

	public MeasureDataBin(long id, float freq, float Rs, float Xs){
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

	public float getFreq(){
		return mFreq;
	}
	public void setFreq(float _freq){
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

	private double calcL(ComplexNumber cxZ, float freq)
	{
		return cxZ.getIm() / (2.0 * Math.PI * (freq / 1000000.0));
	}

	private double calcC(ComplexNumber cxZ, float freq)
	{
		if (cxZ.getIm() == 0)
			return -99999.99;
		return -1000000.0 / (cxZ.getIm() * 2.0 * Math.PI * (freq / 1000000.0));
	}
}
