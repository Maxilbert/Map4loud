package com.example.yuan.classes;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;


import org.jtransforms.fft.DoubleFFT_1D;

/**
 * Created by yuan on 11/13/15.
 */
public class SoundMeter implements Runnable {

    public Handler mMeterHandler;
    public Handler mCurrentDecibelHandler;
    public double calibration = 4.0;
    private int duration = 20000;

    private int bufferSize;
    private double decibel = 0;
    private File fpath = null;
    private File wavFile;
    private File pcmFile;
    private boolean isRecording = true;
    private boolean isGivenUp = true;
    private int frequence = 36000;
    private int channelConfig = AudioFormat.CHANNEL_IN_MONO;
    private int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;


    //This is the interface for GUI
    public SoundMeter (Handler mMeterHandler, Handler mCurrentDecibelHandler, double calibration, int duration) {
        this.calibration = calibration;
        this.mMeterHandler = mMeterHandler;
        this.mCurrentDecibelHandler = mCurrentDecibelHandler;
        this.duration = duration;

        //在这里我们创建一个文件，用于保存录制内容
        fpath = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Android/data/com.example.yuan.map4loud/cache/");
        fpath.mkdirs();//创建文件夹
        try {
            //创建临时文件,注意这里的格式为.pcm
            pcmFile = File.createTempFile("sampling", ".pcm", fpath);
            wavFile = File.createTempFile("sampling", ".wav", fpath);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    //This is the interface for background service
    public SoundMeter(Handler mTotDecibelHandler, double calibration, int duration){
        this(mTotDecibelHandler, null, calibration, duration);
    }


    @Override
    public void run() {

        isRecording = true;
        int count = 0;
        AudioRecord record = null;
        DataOutputStream dos = null;
        Meter meter = null;

        try {
            //开通输出流到指定的文件
             dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(pcmFile)));
            //根据定义好的几个配置，来获取合适的缓冲大小
            bufferSize = 32768;//AudioRecord.getMinBufferSize(frequence, channelConfig, audioEncoding);
            Log.d("Record", "bufferSize = " + bufferSize);
            //实例化AudioRecord
            record = new AudioRecord(MediaRecorder.AudioSource.MIC, frequence, channelConfig, audioEncoding, bufferSize);
            //定义缓冲
            short[] buffer = new short[bufferSize];
            //开始录制
            if(record != null) {
                record.startRecording();
            }
            isRecording = true;
            //15秒后停止录音
            new Timer().schedule(new TimerTask() {
                public void run() {
                    isRecording = false;
                    isGivenUp = false;
                }
            }, duration);
            //定义循环，根据isRecording的值来判断是否继续录制
            while(isRecording){
                //从bufferSize中读取字节，返回读取的short个数
                //这里老是出现buffer overflow，不知道是什么原因，试了好几个值，都没用，TODO：待解决
                int bufferReadResult = record.read(buffer, 0, buffer.length);
                double[] toTransform = new double[bufferReadResult];
                //循环将buffer中的音频数据写入到OutputStream中
                //Log.d("Record", "bufferReadResult = " + bufferReadResult);
                for(int i=0; i<bufferSize; i++) {
                    toTransform[i] = (double) buffer[i] / 32768.0; //归一化
                }
                byte byteBuffer[] = short2byte(buffer);
                dos.write(byteBuffer, 0, bufferSize*2);
                count++;
                meter = new Meter(bufferReadResult, toTransform, frequence);
                new Thread(meter).start();
                //
            }
            //录制结束
            record.stop();
            record.release();
            dos.close();
            Thread.sleep(50);
        } catch (Exception e) {
            // TODO: handle exception
        }
        if(!isGivenUp) {
            copyWaveFile(pcmFile.getAbsolutePath(), wavFile.getAbsolutePath());
            pcmFile.delete();
            Message msg = new Message();
            Bundle data = new Bundle();
            decibel = 10 * Math.log10(decibel / count);
            data.putDouble("dBA", decibel);
            data.putString("audioFile", wavFile.toString());
            msg.setData(data);
            mMeterHandler.sendMessage(msg);
        } else {
            decibel = 0.0;
            Message msg = new Message();
            Bundle data = new Bundle();
            data.putDouble("dBA", decibel);
            msg.setData(data);
            mMeterHandler.sendMessage(msg);
            try {
                record.stop();
                record.release();
                dos.close();
                Thread.sleep(50);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (IllegalStateException e){
                e.printStackTrace();
            }
        }
    }

    public void stop () {
        isRecording = false;
        isGivenUp = true;
    }

    class Meter implements Runnable {

        int bufferSize;
        double[] audioBuffer;
        float sampleRate;

        Meter(int bufferSize, double[] audioBuffer, float sampleRate){
            this.bufferSize = bufferSize;
            this.audioBuffer = audioBuffer;
            this.sampleRate = sampleRate;
        }

        @Override
        public void run() {
            //decibel if from outer class
            decibel  += Math.pow(10, 0.1 * calDecibel()); //db average summation
        }


        //calculate the Decibel of each buffer
        private double calDecibel() {

            final int nFreq = 30; //The number of frequency range
            double dBA = 0;
            double[] f0 = {20.0, 25.0, 31.5, 40.0, 50.0, 63.0, 80.0, 100.0, 125.0, 160.0,
                    200.0, 250.0, 315.0, 400.0, 500.0, 630.0, 800.0, 1000.0, 1250.0, 1600.0,
                    2000.0, 2500.0, 3150.0, 4000.0, 5000.0, 6300.0, 8000.0, 10000.0, 12500.0, 16000.0};
            double[] aWeights = {-50.5, -44.7, -39.4, -34.6, -30.2, -26.2, -22.5, -19.1, -16.1, -13.4,
                    -10.9, -8.6, -6.6, -4.8, -3.2, -1.9, -0.8, 0, 0.6, 1.0,
                    1.2, 1.3, 1.2, 1.0, 0.5, -0.1, -1.1, -2.5, -4.3, -6.6};
            double[] sp = new double[30]; //Sound pressure for each frequency range
            double[] dB = new double[30]; //dB for each frequency range
            double[] freqBuffer = new double[bufferSize];
            double window;
            double arg = 2.0 * Math.PI / (double) (bufferSize - 1);
            for (int i = 0; i < bufferSize; i++) {
                window = (0.5 - 0.5 * Math.cos(arg * (double) i));
                audioBuffer[i] = 1.633 * audioBuffer[i] * window;
            }


            int FFT_SIZE = bufferSize;
            DoubleFFT_1D mFFT = new DoubleFFT_1D(FFT_SIZE); //this is a jTransforms type
            mFFT.realForward(audioBuffer);

            for (int i = 0; i < nFreq; i++) {

                double freqMin = f0[i] / 1.1225;
                double freqMax = f0[i] * 1.1225;
                int nl = (int) Math.round(freqMin * FFT_SIZE / sampleRate);
                int nu = (int) Math.round(freqMax * FFT_SIZE / sampleRate);
                for (int j = nl; j <=nu; j++){
                    if(nu > FFT_SIZE / 2)
                        break;
                    freqBuffer[2*j] = audioBuffer[2*j];
                    freqBuffer[2*j+1] = audioBuffer[2*j+1];
                }
                //Take the inverse FFT to convert signal from frequency to time domain
                mFFT.realInverse(freqBuffer, true);
                sp[i] = Math.sqrt(var(freqBuffer));
                dB[i] = 20 * Math.log10(calibration * sp[i] / 0.00002d) + aWeights[i];
                //dB[i] = 20*Math.log10(sp[i] / 0.0000000001d);
            }
            for (int i = 0; i < nFreq; i++) {
                dBA += Math.pow(10, 0.1 * dB[i]);
            }
            dBA = 10 * Math.log10(dBA);

            Message msg = new Message();
            Bundle data = new Bundle();
            data.putDouble("dBA", dBA);
            msg.setData(data);
            if(mCurrentDecibelHandler != null)
                mCurrentDecibelHandler.sendMessage(msg);
            return dBA;
        }

        //Calculate Variance since it is dB
        private double var(double[] a) {
            double var = 0;
            double mean = 0;
            int n = a.length;
            for (int i = 0; i < n; i++) {
                mean += a[i];
            }
            mean = mean / n;
            for (int i = 0; i < n; i++) {
                var += (a[i] - mean) * (a[i] - mean);
            }
            return var / n;
        }
    }

    //Copy raw pcm format as wav format
    private void copyWaveFile(String inFilename, String outFilename) {
        FileInputStream in;
        FileOutputStream out;
        int bitsPerSample = 16;
        long totalAudioLen;
        long totalDataLen;
        long longSampleRate = frequence;
        int channels = 1;
        long byteRate = bitsPerSample * frequence * channels / 8;


        byte[] data = new byte[bufferSize];

        int silentStuffSize = 9000;
        byte[] silent = new byte[silentStuffSize];
        for(int i=0;i<silentStuffSize;i++){
            silent[i] = 0;
        }

        try {
            in = new FileInputStream(inFilename);
            out = new FileOutputStream(outFilename);
            totalAudioLen = in.getChannel().size() + 2*silentStuffSize;
            totalDataLen = totalAudioLen + 36;
            WriteWaveFileHeader(out, totalAudioLen, totalDataLen,
                    longSampleRate, channels, byteRate);

            out.write(silent);
            while (in.read(data) != -1) {
                out.write(data);
            }
            out.write(silent);

            in.close();
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    //Add the wav header within the raw pcm file
    private void WriteWaveFileHeader (FileOutputStream out, long totalAudioLen, long totalDataLen,
                                      long longSampleRate, int channels, long byteRate)
            throws IOException {

        byte[] header = new byte[44];
        final int bitsPerSample = 16;
        //0-3 big endian
        header[0] = 'R'; // RIFF/WAVE header
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        //4-7 small endian
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        //8-15 big endian
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        header[12] = 'f'; // 'fmt ' chunk
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        //16-35 little endian
        header[16] = 16; // 4 bytes: size of 'fmt ' chunk
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = 1; // 2 bytes: format = 1
        header[21] = 0;
        header[22] = (byte) channels; // 2 bytes: channel number
        header[23] = 0;
        header[24] = (byte) (longSampleRate & 0xff); //4 bytes: sample rate
        header[25] = (byte) ((longSampleRate >> 8) & 0xff);
        header[26] = (byte) ((longSampleRate >> 16) & 0xff);
        header[27] = (byte) ((longSampleRate >> 24) & 0xff);
        header[28] = (byte) (byteRate & 0xff); //4 bytes: byte rate
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        header[32] = (byte) (channels * 16 / 8); // 2 byte: block align, the number of bytes for each sample
        header[33] = 0;
        header[34] = bitsPerSample; // 2 bytes: bits per sample
        header[35] = 0;
        //36-39 little endian
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        //40-43 big endian
        header[40] = (byte) (totalAudioLen & 0xff);
        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);

        out.write(header, 0, 44);
    }

    //convert short to byte
    private byte[] short2byte(short[] sData) {
        int shortArrSize = sData.length;
        byte[] bytes = new byte[shortArrSize * 2];
        for (int i = 0; i < shortArrSize; i++) {
            bytes[i * 2] = (byte) (sData[i] & 0x00FF);
            bytes[(i * 2) + 1] = (byte) (sData[i] >> 8);
            sData[i] = 0;
        }
        return bytes;
    }

}