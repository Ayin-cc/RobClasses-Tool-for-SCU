package com.ayin;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class GetCourse {
    private WebDriver driver = new ChromeDriver();
    private Scanner scanner = new Scanner(System.in);
    private int N = 0;
    private List<String> cid = new ArrayList<>();  // 课程编号
    private List<String[]> cnum = new ArrayList<>();  // 课序号
    private boolean geted = false;
    private List<Integer> defaultNum = new ArrayList<>();  // 是否使用默认课序号，即查询结果第一个
    private Properties properties = new Properties();
    private String studentId;
    private String password;
    private int interval;
    private final static String loginPageUrl = "http://zhjw.scu.edu.cn/login";

    public WebDriver getDriver(){
        return this.driver;
    }

    public GetCourse() {
        // 设置Driver
        System.setProperty("webdriver.chrome.driver", "C:\\Program Files\\Google\\Chrome\\Application\\chromedriver\\chromedriver.exe");

        // 加载属性文件
        try (FileInputStream fileInputStream = new FileInputStream(this.getClass().getClassLoader().getResource("config.properties").getPath())) {
            properties.load(fileInputStream);
        } catch (FileNotFoundException e){
            System.out.println("配置文件不存在，将创建新的属性文件并读入");
            try {
                File file = new File("config.properties");
                file.createNewFile();
                // 写入默认属性
                properties.setProperty("studentId", "0");
                properties.setProperty("password", "0");
                properties.setProperty("init", "0");
                properties.setProperty("interval", "0");
                // 写入到新的属性文件
                try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
                    properties.store(fileOutputStream, "Default properties");
                }
                // 重新加载属性文件
                properties.load(new FileInputStream(file));
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(properties.getProperty("init").equals("0")){
            try {
                FileOutputStream fileOutputStream = new FileOutputStream("config.properties");
                System.out.println("请输入学号：");
                String studentId = scanner.nextLine();
                System.out.println("请输入密码：");
                String password = scanner.nextLine();
                properties.setProperty("studentId", studentId);
                properties.setProperty("password", password);
                properties.setProperty("init", "1");
                properties.setProperty("interval", "2000");
                properties.store(fileOutputStream, "Default properties");
            } catch (Exception e) {
                System.out.println("配置初始化失败");
                return;
            }
        }
        this.studentId = properties.getProperty("studentId");
        this.password = properties.getProperty("password");
        try {
            this.interval = Integer.parseInt(properties.getProperty("interval"));
        } catch (NumberFormatException e) {
            System.out.println("请检查配置文件，时间间隔只能为整型(毫秒)");
        }

        driver.navigate().to("http://zhjw.scu.edu.cn/login");
    }

    public void run(){
        while(true) {
            System.out.println("输入查询课程数(输入0修改学号密码)：");
            N = scanner.nextInt();
            scanner.nextLine();

            if (N == 0) {
                System.out.println("请输入学号：");
                String studentId = scanner.nextLine();
                System.out.println("请输入密码：");
                String password = scanner.nextLine();
                properties.setProperty("studentId", studentId);
                properties.setProperty("password", password);

                try (FileOutputStream fileOutputStream = new FileOutputStream("config.properties")) {
                    properties.store(fileOutputStream, null);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            else{
                break;
            }
        }
        System.out.println("请依次输入查询课程编号");
        for (int i = 0; i < N; i++) {
            String ccid = scanner.nextLine();
            if(ccid.contains("_")){
                ccid = ccid.split("_")[0];
            }
            cid.add(ccid);
        }
        System.out.println("请输入要查询的课序号，每行依次对应一个课程编号，可多选，用空格隔开，输入-1则默认查询结果的第一个：");
        for(int i = 0; i < N; i++){
            String ccum = scanner.nextLine();
            if(ccum.equals("-1")){
                defaultNum.add(1);
            }
            else{
                defaultNum.add(0);
                cnum.add(ccum.split(" "));
            }
        }


        login(studentId, password, driver);
        driver.navigate().to("http://zhjw.scu.edu.cn/student/courseSelect/courseSelect/index");

        // 进入自由选课
        WebElement freeXk = driver.findElement(By.xpath("//li[@id='zyxk']/a"));
        freeXk.click();

        WebElement frameElement = driver.findElement(By.id("ifra"));
        driver.switchTo().frame(frameElement);

        while(true) {
            for(int i = 0; i < cid.size(); i++) {
                if(driver.getCurrentUrl().equals(loginPageUrl)){
                    login(studentId, password, driver);
                    driver.navigate().to("http://zhjw.scu.edu.cn/student/courseSelect/courseSelect/index");

                    // 进入自由选课
                    WebElement nfreeXk = driver.findElement(By.xpath("//li[@id='zyxk']/a"));
                    nfreeXk.click();

                    WebElement nframeElement = driver.findElement(By.id("ifra"));
                    driver.switchTo().frame(nframeElement);
                }

                // 查询课程
                searchCourse(driver, cid.get(i));

                // 查看是否可选
                WebElement courseTableDiv = driver.findElement(By.id("xirxkxkbody"));
                String courseTable = courseTableDiv.getText();

                if(defaultNum.get(i) == 0) {  // 非默认课序号
                    for (int j = 0; j < cnum.get(i).length; j++) {
                        if (!checkCourseId(courseTable, cid.get(i))) {
                            continue;
                        }
                        // 课程可选，click复选框并提交
                        if (subscribe(driver, cid.get(i), cnum.get(i)) == 1) {
                            N--;
                            cid.remove(i);
                        }
                    }
                }
                else{  // 默认课序号
                    if (!checkCourseId(courseTable, cid.get(i))){
                        continue;
                    }
                    // 课程可选，click复选框并提交
                    if (subscribe(driver) == 1) {
                        System.out.println(cid.get(i) + " 已选中!");
                        N--;
                        cid.remove(i);
                    }
                }

                // 是否全部选中
                if(cid.size() == 0){
                    geted = true;
                }
            }

            if (geted) {
                System.out.println("~~~所有课程均已选上~~~");
                break;
            }
        }

        driver.quit();
    }

    private void login(String studentId, String password, WebDriver driver){
        Scanner scanner = new Scanner(System.in);
        while(true) {
            System.out.println("输入验证码：");
            String verification = scanner.nextLine();

            WebElement studentIdInput = driver.findElement(By.id("input_username"));
            WebElement passwordInput = driver.findElement(By.id("input_password"));
            WebElement verificationInput = driver.findElement(By.id("input_checkcode"));

            studentIdInput.sendKeys(studentId);
            passwordInput.sendKeys(password);
            verificationInput.sendKeys(verification);

            WebElement loginButton = driver.findElement(By.id("loginButton"));
            loginButton.click();

            boolean isLoginSuccessful = driver.getPageSource().contains("四川大学教学管理与服务平台");
            if (isLoginSuccessful) {
                break;
            } else {
                System.out.println("输入错误!");
                driver.navigate().to("http://zhjw.scu.edu.cn/login");
            }
        }

    }

    private boolean checkTime(WebDriver driver){
        // 获取HTML
        WebElement element = driver.findElement(By.id("page-content-template"));
        String htmlContent = element.getAttribute("innerHTML").toString();

        if(htmlContent.contains("对不起，当前为非选课阶段！")){
            return false;
        }
        else{
            return  true;
        }
    }

    private void searchCourse(WebDriver driver, String cid){
        // 输入课程编号
        WebElement courseNumberInput = driver.findElement(By.id("kch"));
        courseNumberInput.clear();
        courseNumberInput.sendKeys(cid);

        // 点击查询按钮
        WebElement searchButton = driver.findElement(By.id("queryButton"));
        searchButton.click();

        waitForRefresh(interval);
    }

    private boolean checkCourseId(String courseTable, String cid){
        // 正则表达式匹配课程编号
        Pattern pattern = Pattern.compile("\\((.*?)\\)");
        Matcher matcher = pattern.matcher(courseTable);

        if (matcher.find()) {
            String extractedNumber = matcher.group(1);
            System.out.println("checkCourseId: 匹配到课程: " + cid);
        } else {
            System.out.println("checkCourseId: 未匹配到课程");
            return false;
        }

        return true;
    }

    private int subscribe(WebDriver driver, String courseId, String[] courseNumber){
        Scanner scanner = new Scanner(System.in);
        // 获取HTML
        WebElement courseTableDiv = driver.findElement(By.id("xirxkxkbody"));
        String courseTable = courseTableDiv.getAttribute("innerHTML").toString();

        // 使用正则表达式匹配括号中的"123_01"格式的数字，并记录下匹配与输入相同的索引
        Pattern pattern = Pattern.compile("\\((\\d+_\\d+)\\)");
        Matcher matcher = pattern.matcher(courseTable);

        Queue<Integer> matchingIndex = new LinkedList<>();

        int index = 0;
        while (matcher.find()) {
            index++;
            String matchedNumber = matcher.group(1);
            for (int i = 0; i < courseNumber.length; i++) {
                if (matchedNumber.equals(courseId + "_" + courseNumber[i])) {
                    matchingIndex.offer(index);
                    break;
                }
            }
        }

        // 判断 Queue 是否为空，若为空，则重新查询
        if (!matchingIndex.isEmpty()) {
            System.out.println("subscribe: 找到符合要求的课程" + courseId + "，正在选取...");
        }
        else{
            System.out.println("subscribe: " + "未找到符合要求的课程" + courseId);
            searchCourse(driver, courseId);
            return -1;
        }



        // 使用正则表达式匹配复选框的id，并根据索引进行勾选
        Pattern idPattern = Pattern.compile("id=\"(.*?)\"");
        Matcher idMatcher = idPattern.matcher(courseTable);

        int checkboxIndex = 0;
        boolean geted = false;

        while (idMatcher.find()) {
            checkboxIndex++;
            String checkboxId = idMatcher.group(1);

            if (checkboxIndex == matchingIndex.peek()) {
                WebElement checkbox = driver.findElement(By.id(checkboxId));
                checkbox.click();
                matchingIndex.remove();

                // 检查是否需要验证码
                driver.switchTo().defaultContent();
                WebElement verification = driver.findElement(By.id("submitCode"));
                boolean needVerification = verification.getAttribute("innerHTML").toString().contains("style=\"display: none;\"");
                if(needVerification){
                    inputVerification(verification);
                }

                // 提交
                WebElement button = driver.findElement(By.id("submitButton"));
                button.click();
                WebElement frameElement = driver.findElement(By.id("ifra"));
                driver.switchTo().frame(frameElement);

                try {
                    waitForRefresh(interval);
                    WebElement isGeted = driver.findElement(By.id("106812020_06"));
                    geted = true;
                } catch(NoSuchElementException e){
                    // 跳转回自由选课界面
                    driver.navigate().to("http://zhjw.scu.edu.cn/student/courseSelect/courseSelect/index");
                    // 进入自由选课
                    WebElement nfreeXk = driver.findElement(By.xpath("//li[@id='zyxk']/a"));
                    nfreeXk.click();

                    WebElement nframeElement = driver.findElement(By.id("ifra"));
                    driver.switchTo().frame(nframeElement);

                    // 根据队列情况判读选择下一个还是重新查询
                    if(matchingIndex.isEmpty()){
                        // 若为空，则重新查询
                        System.out.println("subscribe: 选取失败！请检查课程时间是否冲突, 按任意键重新查询...");
                        scanner.nextLine();
                        searchCourse(driver, courseId);
                        subscribe(driver, courseId, courseNumber);
                    }
                    else{
                        // 不空，则查询下一个课序号
                        System.out.println("subscribe: 选取失败！尝试选择下一课序号。");
                        searchCourse(driver, courseId);
                    }
                }

                if(geted){
                    break;
                }
            }
        }
        if(geted) {
            return  1;
        }
        else{
            return -1;
        }
    }

    private int subscribe(WebDriver driver){
        // 获取HTML
        WebElement courseTableDiv = driver.findElement(By.id("xirxkxkbody"));
        String courseTable = courseTableDiv.getAttribute("innerHTML").toString();
        System.out.println("innerHtml" + courseTable);

        // 使用正则表达式匹配复选框的id，并勾选查询到的第一个
        Pattern idPattern = Pattern.compile("id=\"(.*?)\"");
        Matcher idMatcher = idPattern.matcher(courseTable);

        if(idMatcher.find()){
            String checkboxId = idMatcher.group(1);
            WebElement checkbox = driver.findElement(By.id(checkboxId));
            checkbox.click();
        }
        else{
            System.out.println("subscribe: 复选框未找到！");
            return -1;
        }

        // 提交
        driver.switchTo().defaultContent();
        WebElement button = driver.findElement(By.id("submitButton"));
        button.click();
        WebElement frameElement = driver.findElement(By.id("ifra"));
        driver.switchTo().frame(frameElement);

        return 1;
    }

    private void inputVerification(WebElement verification){
        Scanner scanner = new Scanner(System.in);

        System.out.println("请输入验证码：");
        String code = scanner.nextLine();

        verification.sendKeys(code);
    }

    private void waitForRefresh(int n){
        try {
            Thread.sleep(n);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}
