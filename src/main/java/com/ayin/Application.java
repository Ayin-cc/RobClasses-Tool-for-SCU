package com.ayin;

public class Application {
    public static void main(String[] args) {
        GetCourse getCourse = new GetCourse();
        try {
            getCourse.run();
        } finally {
            getCourse.getDriver().quit();
        }
    }
}
