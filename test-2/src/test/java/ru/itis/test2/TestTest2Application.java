package ru.itis.test2;

import org.springframework.boot.SpringApplication;

public class TestTest2Application {

    public static void main(String[] args) {
        SpringApplication.from(Test2Application::main).with(TestcontainersConfiguration.class).run(args);
    }

}
