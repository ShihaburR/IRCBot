package main.java;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

public class ircMain {
    //static objects to be used in other methods other than main
    private static Scanner input;
    private static PrintWriter output;

    private static String nick;
    private static String user;
    private static String fullname;
    private static String channel;

    //connection and user is setup here
    public static void main(String[] args) throws IOException {
        //Collect user input of name, nickname and channel they want to join
        Scanner console = new Scanner(System.in);
        System.out.print("Enter a nickname: ");
        nick = console.nextLine();
        System.out.print("Enter a username: ");
        user = console.nextLine();
        System.out.print("Enter your full name: ");
        fullname = console.nextLine();
        System.out.print("Enter a channel: ");
        channel = console.nextLine();

        //setup of socket to IRC
        Socket socket = new Socket("chat.freenode.net", 6667);
        output = new PrintWriter(socket.getOutputStream(), true);
        input = new Scanner(socket.getInputStream());

        //send messages to server to establish a connection
        writeMessage("NICK", nick);
        writeMessage("USER", user + " 0 * :" + fullname);
        writeMessage("JOIN",channel);

        //output all messages received from server until program ends
        while(input.hasNext()){
            String serverMessage = input.nextLine();
            System.out.println("<<< " + serverMessage);
            //ends the program if the nickname is already in use
            if(serverMessage.contains(".net 433")) {
                String error = serverMessage.substring(serverMessage.indexOf("433") + 1);
                System.out.println("-----------Error: " + error + "--------------");
                break;
            }
            //captures any errors and tells the channel that may occur when executing commands
            else if(serverMessage.contains(".net 4")){
                String error = serverMessage.substring(serverMessage.indexOf(" :")+1);
                writeMessage("PRIVMSG " + channel, ":ERROR - " + error);
            } else {
                //executes commands based on other users on IRC
                checkCommands(serverMessage);
                //gets server data based on commands for users to see
                getIRCInfo(serverMessage);
            }
        }

        //closes all writers and socket connection
        input.close();
        output.close();
        socket.close();
        System.out.println("Connection has ended");
    }

    //writes commands with any message for output stream to use for the IRC
    private static void writeMessage(String command, String message) {
        String msg = command + " " + message;
        //this is display in the console what we want to do
        System.out.println(">>> " + msg);
        //ensure it is done in IRC and a new line is set to prevent any errors
        output.print(msg + "\r\n");
        //flush all contents of the output stream so the method can be called again without previous messages.
        output.flush();
    }

    //checks commands within server messages
    private static void checkCommands(String message) {
        //reply to a specific message with a greeting
        if(message.contains("Hello " + nick)) {
            String user = message.substring(1,message.indexOf("!"));
            writeMessage("PRIVMSG " + channel,":Hello, " + user + " How can I help you today? (message !help for commands)");
        }
        //this allows the IRC know the bot is still active
        else if(message.startsWith("PING")){
            String pingContents = message.split(" ",2)[1];
            writeMessage("PONG",pingContents);
        }
        //this displays all available commands the bot can do
        else if(message.contains("!help")){
            writeMessage("PRIVMSG " + channel, ":I can do many things, " +
                    "!goodbye = remove bot from server, !timenow = tell the time currently on the server, " +
                    "!invite <nickname> = invite a user to the channel (if you are channel admin), " +
                    "!users <channel> = lists all users in the server, !humour = get told the funniest jokes ever");
        }
        //this disconnects the bot from the server, ending the program here
        else if(message.contains("!goodbye")){
            writeMessage("PRIVMSG " + channel, ":Disconnecting from server... Goodbye");
            writeMessage("QUIT", "");
        }
        //this gets the current time that is used within the loop to display to the user
        else if(message.contains("!timenow")){
            writeMessage("TIME" + channel, "");
        }
        //this only works if you are channel manager where you can invite other members
        else if(message.contains("!invite")){
            String user = message.substring(message.indexOf(" ")+1);
            writeMessage("PRIVMSG" + channel,":Inviting " + user + " to current server");
            writeMessage("INVITE",user + " " + channel);
        }
        //lists all users in a specific server
        else if(message.contains("!users")){
            String server = message.substring(message.indexOf("s #")+1);
            writeMessage("NAMES",server);
        }
        //tells a random joke to the user
        else if(message.contains("!humour")){
            //calls the generate joke method in a try and catch to handle file not found errors
            try {
                generateJoke();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    //collects data from server messages
    private static void getIRCInfo(String serverMessage) {
        //allow the display of the time to the user
        if(serverMessage.contains(".net 391")){
            String time = serverMessage.substring(serverMessage.indexOf(":S")+1);
            writeMessage("PRIVMSG " + channel, ":" + time);
        }
        //allows the display of users in a specified channel
        else if(serverMessage.contains(".net 353")){
            String users = serverMessage.substring(serverMessage.indexOf("= ")+1);
            writeMessage("PRIVMSG " + channel, ":" + users);
        }
    }

    //generate random jokes from a txt file
    private static void generateJoke() throws FileNotFoundException {
        List<String> jokes = new ArrayList<>();
        Random rand = new Random();
        String joke = "";
        Scanner inFile = new Scanner(new File("jokes.txt"));
        //collects all jokes into array
        while(inFile.hasNext()){
            joke = inFile.nextLine();
            jokes.add(joke);
        }
        //initialise random number within the array's size to prevent outofbound errors
        int rnd = rand.nextInt(jokes.size());
        //send message of joke from array based on random number
        writeMessage("PRIVMSG " + channel,":" + jokes.get(rnd));
    }

}

