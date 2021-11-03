package org.example;

public class ServerStart {

        public static void main(String[] args) {
           Server server = new Server(64);
           server.listen(2222);

        }
    }
