/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.marianatrench;

import com.facebook.common.preconditions.Preconditions;
import com.facebook.infer.annotation.Nullsafe;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

@Nullsafe(Nullsafe.Mode.LOCAL)
public class Desugar {
  public static void main(final String[] arguments) throws IOException, FileNotFoundException {
    System.out.printf("Desugaring file %s into file %s%n", arguments[0], arguments[1]);

    ArrayList<String> skippedClasses = new ArrayList<>();
    if (arguments.length > 2) {
      System.out.printf("User-configured skipped file: %s%n", arguments[2]);
      System.out.println("Methods in classes with the following prefixes will be made empty.");
      try (BufferedReader reader = new BufferedReader(new FileReader(arguments[2]))) {
        String line;
        while ((line = reader.readLine()) != null) {
          System.out.println(line);
          skippedClasses.add(line);
        }
      }
    }

    // NULLSAFE_FIXME[Not Vetted Third-Party]
    Path temporaryDirectory = Files.createTempDirectory("");

    try (JarFile inputJar = new JarFile(arguments[0])) {
      Enumeration<JarEntry> entries = inputJar.entries();
      while (entries.hasMoreElements()) {
        JarEntry entry = entries.nextElement();

        File file =
            new File(
                temporaryDirectory + File.separator + Preconditions.checkNotNull(entry).getName());

        File parentDir = file.getParentFile();
        if (!Preconditions.checkNotNull(parentDir).exists()) {
          Preconditions.checkNotNull(parentDir).mkdirs();
        }
        if (entry.isDirectory()) {
          continue;
        }
        try (InputStream inputStream = inputJar.getInputStream(entry)) {
          while (inputStream.available() > 0 && file.getName().endsWith(".class")) {
            try (FileOutputStream outputStream = new FileOutputStream(file)) {
              ClassReader reader = new ClassReader(inputStream);
              // Must not set COMPUTE_FRAMES, because some classes are loaded from classloaders that
              // are not the bootstrap classloader and this would cause ASM to be unable to find
              // those classes.
              ClassWriter writer = new ClassWriter(/* no flags */ 0);
              NestVisitor nestVisitor = new NestVisitor(writer);
              MethodHandleVisitor methodHandleVisitor =
                  new MethodHandleVisitor(nestVisitor, skippedClasses);
              reader.accept(methodHandleVisitor, /* no options */ 0);

              outputStream.write(writer.toByteArray());
            }
          }
        }
      }
    }

    String[] command = {"jar", "cf", arguments[1], "."};
    RunCommand.run(
        Runtime.getRuntime()
            // NULLSAFE_FIXME[Parameter Not Nullable, Not Vetted Third-Party]
            .exec(command, null, new File(temporaryDirectory.toAbsolutePath().toString())));

    try (Stream<Path> walk = Files.walk(temporaryDirectory)) {
      walk.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
    }
  }
}
