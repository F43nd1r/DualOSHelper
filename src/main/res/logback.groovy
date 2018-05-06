def USER_HOME = System.getProperty("user.home");

appender("FILE", FileAppender) {
    file = "$USER_HOME/.clipboardshare/out.log"
    encoder(PatternLayoutEncoder) {
        pattern = "%d{HH:mm:ss.SSS} %-5level %logger{5} - %msg%n"
    }
}

appender("STDOUT", ConsoleAppender) {
    encoder(PatternLayoutEncoder){
        pattern = "%msg%n"
    }
}

root(DEBUG, ["FILE", "STDOUT"])