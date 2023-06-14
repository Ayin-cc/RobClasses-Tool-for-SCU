import graphql.org.antlr.v4.runtime.misc.IntegerList;
import graphql.util.EscapeUtil;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;
import sun.audio.AudioPlayer;
import sun.audio.AudioStream;

import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;


public class GetCourse {
    public static void main(String[] args) {
        String loginPageUrl = "http://zhjw.scu.edu.cn/login";
        System.setProperty("webdriver.chrome.driver", "C:/Program Files/Google/Chrome/Application/chromedriver/chromedriver.exe");

        WebDriver driver = new ChromeDriver();
        Scanner scanner = new Scanner(System.in);

        // 加载属性文件
        Properties properties = new Properties();
        try (FileInputStream fileInputStream = new FileInputStream("config.properties")) {
            properties.load(fileInputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if(properties.getProperty("init").equals("0")){
            System.out.println("请输入学号：");
            String account = scanner.nextLine();
            System.out.println("请输入密码：");
            String password = scanner.nextLine();
            properties.setProperty("account", account);
            properties.setProperty("password", password);
            properties.setProperty("init", "1");
        }

        try (FileOutputStream fileOutputStream = new FileOutputStream("config.properties")) {
            properties.store(fileOutputStream, null);
        } catch (IOException e) {
            e.printStackTrace();
        }


        int N = 0;
        List<String> cid = new ArrayList<>();   // 课程编号
        List<String[]> cnum = new ArrayList<>();    // 课序号
        boolean geted = false;
        IntegerList defaultNum = new IntegerList(); // 是否使用默认课序号，即查询结果第一个

        while(true) {
            System.out.println("输入查询课程数(输入0修改学号密码)：");
            N = scanner.nextInt();
            scanner.nextLine();

            if (N == 0) {
                System.out.println("请输入学号：");
                String account = scanner.nextLine();
                System.out.println("请输入密码：");
                String password = scanner.nextLine();
                properties.setProperty("account", account);
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

        String account = properties.getProperty("account");
        String password = properties.getProperty("password");

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

        login(account, password, driver);
        driver.navigate().to("http://zhjw.scu.edu.cn/student/courseSelect/courseSelect/index");

        // 检查是否为选课时间
        if(!checkTime(driver)){
            System.out.println("对不起，当前为非选课阶段！");
            scanner.nextLine();
            return;
        }

        // 进入自由选课
        WebElement freeXk = driver.findElement(By.xpath("//li[@id='zyxk']/a"));
        freeXk.click();

        WebElement frameElement = driver.findElement(By.id("ifra"));
        driver.switchTo().frame(frameElement);

        while(true) {
            for(int i = 0; i < cid.size(); i++) {
                if(driver.getCurrentUrl().equals(loginPageUrl)){
                    login(account, password, driver);
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
                System.out.println("courseTable: " + courseTable);

                if(defaultNum.get(i) == 0) {
                    for (int j = 0; j < cnum.get(i).length; j++) {
                        if (!checkCourseId(courseTable, cid.get(i) + "_" + cnum.get(i)[j])) {
                            continue;
                        } else {
                            // 课程可选，click复选框并提交
                            if (subscribe(driver, cid.get(i), cnum.get(i)) == 1) {
                                System.out.println(cid.get(i) + " 已选中，按下任意键继续...");
                                scanner.nextLine();

                                N--;
                                cid.remove(i);
                            }
                        }
                    }
                }
                else{
                    if (subscribe(driver) == 1) {
                        System.out.println(cid.get(i) + " 已选中，按下任意键继续...");
                        scanner.nextLine();

                        N--;
                        cid.remove(i);
                    }
                }

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

    public static void login(String account, String password, WebDriver driver){
        Scanner scanner = new Scanner(System.in);
        driver.navigate().to("http://zhjw.scu.edu.cn/login");

        while(true) {
            System.out.println("输入验证码：");
            String verification = scanner.nextLine();

            WebElement accountInput = driver.findElement(By.id("input_username"));
            WebElement passwordInput = driver.findElement(By.id("input_password"));
            WebElement verificationInput = driver.findElement(By.id("input_checkcode"));

            accountInput.sendKeys(account);
            passwordInput.sendKeys(password);
            verificationInput.sendKeys(verification);

            WebElement loginButton = driver.findElement(By.id("loginButton"));
            loginButton.click();

            boolean isLoginSuccessful = driver.getPageSource().contains("URP综合教务系统首页");
            if (isLoginSuccessful) {
                break;
            } else {
                System.out.println("输入错误!");
                driver.navigate().to("http://zhjw.scu.edu.cn/login");
            }
        }

    }

    public static boolean checkTime(WebDriver driver){
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

    public static void searchCourse(WebDriver driver, String cid){
        // 输入课程编号
        WebElement courseNumberInput = driver.findElement(By.id("kch"));
        courseNumberInput.clear();
        courseNumberInput.sendKeys(cid);

        // 点击查询按钮
        WebElement searchButton = driver.findElement(By.id("queryButton"));
        searchButton.click();

        waitForRefresh(2);
    }

    public static boolean checkCourseId(String courseTable, String cid){
        // 正则表达式匹配课程编号
        Pattern pattern = Pattern.compile("\\((.*?)\\)");
        Matcher matcher = pattern.matcher(courseTable);

        if (matcher.find()) {
            String extractedNumber = matcher.group(1);
            System.out.println("checkCourseId: 匹配到课程: " + extractedNumber);
        } else {
            System.out.println("checkCourseId: 未匹配到课程");
            return false;
        }

        return true;
    }

    public static int subscribe(WebDriver driver, String courseId, String[] courseNumber){
        // 获取HTML
        WebElement courseTableDiv = driver.findElement(By.id("xirxkxkbody"));
        String courseTable = courseTableDiv.getAttribute("innerHTML").toString();
        System.out.println("innerHtml" + courseTable);

        // 使用正则表达式匹配括号中的"123_01"格式的数字，并记录下匹配与输入相同的索引
        Pattern pattern = Pattern.compile("\\((\\d+_\\d+)\\)");
        Matcher matcher = pattern.matcher(courseTable);

        Queue<Integer> matchingIndex = new LinkedList<>();
        int index = 0;
        while (matcher.find()) {
            index++;
            String matchedNumber = matcher.group(1);
            for(int i = 0; i < courseNumber.length; i++) {
                if (matchedNumber.equals(courseId + "_" + courseNumber[i])) {
                    matchingIndex.offer(index);
                    break;
                }
            }
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
                    waitForRefresh(2);
                    WebElement isGeted = driver.findElement(By.id("106812020_06"));
                    geted = true;
                } catch(NoSuchElementException e){
                    // 没有成功选上，选择下一个
                    driver.navigate().to("http://zhjw.scu.edu.cn/student/courseSelect/courseSelect/index");
                    // 进入自由选课
                    WebElement nfreeXk = driver.findElement(By.xpath("//li[@id='zyxk']/a"));
                    nfreeXk.click();

                    WebElement nframeElement = driver.findElement(By.id("ifra"));
                    driver.switchTo().frame(nframeElement);

                    searchCourse(driver, courseId);
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

    public static int subscribe(WebDriver driver){
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

    public static void inputVerification(WebElement verification){
        Scanner scanner = new Scanner(System.in);

        System.out.println("请输入验证码：");
        String code = scanner.nextLine();

        verification.sendKeys(code);
    }

    public static void waitForRefresh(int n){
        try {
            Thread.sleep(1000 * n);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}
