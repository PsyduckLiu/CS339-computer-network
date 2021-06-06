package com.modules;

import android.util.Log;

public class GenBinaryText {
    private static final String TAG = "GenBinaryText";

    public GenBinaryText(){
        //TODO
    }

    public String convertTextToBinary(String text) {
        CRCChecker myCRCchecker = new CRCChecker();
        StringBuilder dataString = new StringBuilder();
        StringBuilder tempString = new StringBuilder();

        char[] charText = text.toCharArray();
        for (int i=0;i < charText.length;i++) {
            tempString.delete(0,tempString.length());

            int ascNumber = Integer.valueOf(charText[i]);
            String numberString = Integer.toString(ascNumber,2);

            int length = numberString.length();
            int append = 8-length;
            while (append > 0 ) {
                dataString.append('0');
                append--;
            }
            dataString.append(numberString);
        }
        Log.d(TAG,"Binary data string is：" + text + '=' + dataString);

        // CRC handle
        StringBuilder CRCString_B = new StringBuilder();
        for(int i = 0;i < dataString.length();i++){
            char ch = dataString.charAt(i);
            CRCString_B.append(ch);
        }
        String myCRCstring = myCRCchecker.AddCRCCode(CRCString_B.toString());
        Log.d(TAG,"After adding CRC , binary data string is: "+ myCRCstring);

        for (int j = 0;j < myCRCstring.length();j++) {
            char ch = myCRCstring.charAt(j);
            int ch_plus = (int)ch;
            tempString.append((char)ch_plus);
        }
        myCRCstring = tempString.toString();

        return myCRCstring;
    }
}
