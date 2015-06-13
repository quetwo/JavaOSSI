package edu.msu.telecom.OSSI;

import com.jcraft.jsch.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

public class SSH
{
    private JSch sshClient;
    private Session sshSession;
    private Channel channel;

    private BufferedReader input;
    private PrintWriter output;

    private boolean debugOutput = false;

    public SSH(String username, String password, String host)
    {
        sshClient = new JSch();
        JSch.setConfig("StrictHostKeyChecking", "no");
        try
        {
            sshSession = sshClient.getSession(username,host,5022);
            sshSession.setPassword(password);
        }
        catch (JSchException e)
        {
            e.printStackTrace();
        }
    }

    public void setDebug(boolean debug)
    {
        this.debugOutput = debug;
    }

    public void connectToServer()
    {
        try
        {
            sshSession.connect(3000);
            channel = sshSession.openChannel("shell");
            channel.connect(3000);
            output = new PrintWriter(channel.getOutputStream());
            input = new BufferedReader(new InputStreamReader(channel.getInputStream()));
        }
        catch (JSchException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        if (debugOutput) System.out.println("[SSH] Connected to host");
    }

    public void disconnectFromServer()
    {
        channel.disconnect();
        sshSession.disconnect();
        if (debugOutput) System.out.println("[SSH] Disconnected from host");
    }

    public boolean dataAvailable()
    {
        try
        {
            return input.ready();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return false;
    }

    public String readLine()
    {
        try
        {
            return input.readLine();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return "";
    }

    public String readChar()
    {
        int incoming = 0;
        try
        {
            incoming = input.read();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return String.valueOf((char) incoming);
    }

    public boolean writeLn(String StringToSend)
    {

        if (debugOutput) System.out.println("=> " + StringToSend);

        try
        {
            output.println(StringToSend);
            output.flush();
        }
        catch (Error e)
        {
            return false;
        }
        return true;
    }

    public boolean write(String StringToSend)
    {
        try
        {
            output.print(StringToSend);
            output.flush();
        }
        catch (Error e)
        {
            return false;
        }
        return true;
    }

}
