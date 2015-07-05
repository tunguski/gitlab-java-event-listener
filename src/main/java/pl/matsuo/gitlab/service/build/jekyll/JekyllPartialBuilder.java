package pl.matsuo.gitlab.service.build.jekyll;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.util.FileUtil;
import org.springframework.stereotype.Service;
import pl.matsuo.gitlab.hook.PartialBuildInfo;
import pl.matsuo.gitlab.hook.PushEvent;
import pl.matsuo.gitlab.service.build.PartialBuilder;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;


/**
 * Created by marek on 04.07.15.
 */
@Service
public class JekyllPartialBuilder extends PartialBuilder {


  public CompletableFuture<PartialBuildInfo> internalExecute(PushEvent pushEvent, Properties properties) {
    PartialBuildInfo partialBuildInfo = new PartialBuildInfo();
    partialBuildInfo.setName(getName());

    CompletableFuture<PartialBuildInfo> future = new CompletableFuture();
    File projectBase = gitRepositoryService.repository(pushEvent);

    String source = JekyllProperties.source(properties);
    String destination = JekyllProperties.destination(properties);

    File siteBase = new File(projectBase, destination);
    if (!siteBase.mkdirs()) {
      try {
        // FileUtils is better at creating directories than JVM ;)
        FileUtils.forceMkdir(siteBase);
      } catch (IOException e) {
        e.printStackTrace();
        throw new RuntimeException("Cannot create site base directory: " + siteBase.getAbsolutePath());
      }
    }
    destination = siteBase.getAbsolutePath();

    // fixme: execute build
    ProcessBuilder pb = new ProcessBuilder("jekyll", "build", "--destination", destination);
    pb.environment().put("ci-build", "true");
    pb.directory(new File(projectBase, source));
    try {
      Process process = pb.start();
      process.waitFor();

      BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));
      BufferedReader stdError = new BufferedReader(new InputStreamReader(process.getErrorStream()));

      String log = "";

      // read the output from the command
      String s = null;
      while ((s = stdInput.readLine()) != null) {
        log = log + "\n" + s;
      }

      log = log + "\n\n";

      // read any errors from the attempted command
      while ((s = stdError.readLine()) != null) {
        log = log + "\n" + s;
      }

      partialBuildInfo.setLog(log);
      if (process.exitValue() == 0) {
        partialBuildInfo.setStatus("ok");
      } else {
        System.out.println(log);
      }

    } catch (Exception e) {
      ByteArrayOutputStream os = new ByteArrayOutputStream();
      e.printStackTrace(new PrintWriter(os));
      partialBuildInfo.setLog(new String(os.toByteArray()));
      e.printStackTrace();
    }

    future.complete(partialBuildInfo);

    return future;
  }


  @Override
  public String getName() {
    return "jekyll";
  }
}

