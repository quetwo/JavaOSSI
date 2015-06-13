package edu.msu.telecom.OSSI;

import java.util.*;

public class OSSI
{

    private SSH sshClient;
    private boolean connected = false;
    private boolean debugOutput = false;

    public OSSI(String username, String password, String host)
    {
        System.out.println("Connecting as an OSSI client to " + host + "....");
        sshClient = new SSH(username, password, host);
    }

    public void setDebug(boolean debugOutput)
    {
        this.debugOutput = debugOutput;
        sshClient.setDebug(debugOutput);
    }

    public boolean connect()
    {
        boolean loggingIn = true;
        boolean waitForTerm = true;
        String curLine = "";

        sshClient.connectToServer();
        if (debugOutput) System.out.println("Waiting for connection to stabilize...");
        try
        {
            Thread.sleep(100);
        }
        catch (InterruptedException e)
        {
            // no idea why we would hit an exception here.
        }

        do
        {
            curLine = curLine + sshClient.readChar();

            if (curLine.contains("PIN"))
            {
                System.err.println("Username / Password correct, but account locked with PIN access.");
                disconnect();
                return false;
            }

            if (curLine.contains("[513]"))
            {
                loggingIn = false;
            }
        }
        while (loggingIn);
        sshClient.writeLn("ossi");

        do
        {
            curLine = sshClient.readLine();

            if(curLine.trim().contains("t"))
            {
                waitForTerm = false;
            }
        }
        while (waitForTerm);

        if (debugOutput) System.out.println("Connected and term set.  Waiting for commands.");
        connected = true;

        return true;
    }

    public boolean disconnect()
    {
        sshClient.disconnectFromServer();
        return true;
    }

    public boolean isConnected()
    {
        return connected;
    }

    public ArrayList<HashMap> sendCommand(String commandToSend)
    {
        return sendCommand(commandToSend, new HashMap());
    }

    public ArrayList<HashMap> sendCommand(String commandToSend, HashMap parameters)
    {
        String incomingBuffer;
        boolean bufferSearch = true;
        int curColIndex = 0;
        ArrayList<String> fieldsList = new ArrayList<String>();
        HashMap outputItem = new HashMap();
        ArrayList<HashMap> outputQuery = new ArrayList<HashMap>();

        if (!connected)
        {
            return outputQuery;
        }

        if (debugOutput) System.out.println("Sending command [" + commandToSend + "]");

        sshClient.writeLn("c" + commandToSend);

        if (parameters.size() > 0)
        {
            String fieldsToSend ="f";
            String dataToSend = "d";

            Iterator<Map.Entry<String,String>> iterator = parameters.entrySet().iterator();
            while(iterator.hasNext())
            {
                Map.Entry<String,String> entry = iterator.next();
                fieldsToSend = fieldsToSend + entry.getKey() + "\t";
                dataToSend = dataToSend + entry.getValue() + "\t";
            }
            sshClient.writeLn(fieldsToSend);
            sshClient.writeLn(dataToSend);
        }

        sshClient.writeLn("t");

        /////////////////////////
        // eat the old buffer. prepare for the command to be run
        /////////////////////////

        incomingBuffer = "";
        do
        {
            incomingBuffer = incomingBuffer + sshClient.readChar();
            if (incomingBuffer.contains(commandToSend))
            {
                bufferSearch = false;
            }
        }
        while(bufferSearch);

        ////////////////////////
        // map the results of the command to a HashMap.
        ////////////////////////

        do
        {
            incomingBuffer = sshClient.readLine();
            if (debugOutput) System.out.println("<= " + incomingBuffer);

            if (incomingBuffer.length() > 0)
            {
                switch (incomingBuffer.charAt(0))
                {
                    case 'f':
                    {
                        // we are now collecting field-names
                        String colListWork = incomingBuffer.substring(1); //eat the 'f' in the beginning of the string
                        String[] fields = colListWork.split("\t"); //split the field names by the tab delims
                        fieldsList.addAll(Arrays.asList(fields));
                        break;
                    }
                    case 'd':
                    {
                        // we are now collecting data from fields
                        String colListWork = incomingBuffer.substring(1) + " "; //eat the 'd' in the beginning of the string
                                                                                //we have to add a space to the end because empty
                                                                                //fields are ignored at the end.
                        String[] fields = colListWork.split("\t"); //split the fields by the tab delims

                        for (int i = 0; i < fields.length; i++)
                        {
                            outputItem.put(fieldsList.get(curColIndex), fields[i]);
                            curColIndex++;
                        }

                        outputItem.put(fieldsList.get(curColIndex - 1), fields[fields.length - 1].substring(0, fields[fields.length - 1].length() - 1)); //eat the last char from the last field that we added earlier.

                        break;
                    }
                    case 'n':
                    {
                        // we just got a new record.
                        outputQuery.add(outputItem);
                        curColIndex = 0;
                        break;
                    }
                    case 'e':
                    {
                        // we got an error from the command.
                        System.err.println("Error : " + incomingBuffer.substring(1));
                        break;
                    }
                }
            }
        }
        while (!incomingBuffer.startsWith("t"));

        outputQuery.add(outputItem);
        return outputQuery;
    }



}
