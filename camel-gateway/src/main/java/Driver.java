
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class Driver {

  public static void main(String[] args) throws Throwable {
    ApplicationContext context = new ClassPathXmlApplicationContext("META-INF/spring/applicationContext.xml");
    while (true) {
      Thread.sleep(1000);
    }
  }
}
