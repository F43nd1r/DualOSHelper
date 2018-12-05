def USER_HOME = System.getProperty("user.home");

appender("FILE", RollingFileAppender) {
    file = "$USER_HOME/.clipboardshare/out.log"
    encoder(PatternLayoutEncoder) {
        pattern = "%d{HH:mm:ss.SSS} %-5level %logger{5} - %msg%n"
    }
    rollingPolicy(TimeBasedRollingPolicy) {
        fileNamePattern = "$USER_HOME/.clipboardshare/out-%d{yyyy-MM-dd-HH}.log"
        maxHistory = 48
    }
}

appender("STDOUT", ConsoleAppender) {
    encoder(PatternLayoutEncoder){
        pattern = "%msg%n"
    }
}

root(DEBUG, ["FILE", "STDOUT"])