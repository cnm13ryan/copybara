// Copyright 2016 Google Inc. All Rights Reserved.
package com.google.copybara.transform;

import com.google.common.base.Preconditions;
import com.google.copybara.config.ConfigValidationException;
import com.google.copybara.util.console.Console;
import com.google.devtools.build.lib.events.Location;
import com.google.devtools.build.lib.syntax.EvalException;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import javax.annotation.Nullable;

/**
 * Transformation the moves (renames) a single file or directory.
 */
public class Move implements Transformation {

  private final String before;
  private final String after;
  private final TransformOptions transformOptions;

  private Move(String before, String after, TransformOptions transformOptions) {
    this.before = Preconditions.checkNotNull(before);
    this.after = Preconditions.checkNotNull(after);
    this.transformOptions = Preconditions.checkNotNull(transformOptions);
  }

  public static Move fromConfig(
      String before, String after, TransformOptions transformOptions, Location location)
      throws EvalException {
    return new Move(
        validatePath(location, before),
        validatePath(location, after),
        transformOptions);
  }

  @Override
  public void transform(Path workdir, Console console) throws IOException, ValidationException {
      console.progress("Moving " + this.before);
      Path before = workdir.resolve(this.before);
      if (!Files.exists(before)) {
        transformOptions.reportNoop(
            console,
            String.format("Error moving '%s'. It doesn't exist in the workdir", this.before));
        return;
      }
      Path after = workdir.resolve(this.after);
      if (Files.isDirectory(after, LinkOption.NOFOLLOW_LINKS)
          && after.startsWith(before)) {
        // When moving from a parent dir to a sub-directory, make sure after doesn't already have
        // files in it - this is most likely a mistake.
        new VerifyDirIsEmptyVisitor(after).walk();
      }
      createParentDirs(after);
      try {
        Files.walkFileTree(before, new MovingVisitor(before, after));
      } catch (FileAlreadyExistsException e) {
        throw new ValidationException(
            String.format("Cannot move file to '%s' because it already exists", e.getFile()));
      }
  }

  @Override
  public Transformation reverse() {
    return new Move(after, before, transformOptions);
  }

  private void createParentDirs(Path after) throws IOException, ValidationException {
    try {
      Files.createDirectories(after.getParent());
    } catch (FileAlreadyExistsException e) {
      // This exception message is particularly bad and we don't want to treat it as unhandled
      throw new ValidationException(String.format(
          "Cannot create '%s' because '%s' already exists and is not a directory",
          after.getParent(), e.getFile()));
    }
  }

  @Override
  public String describe() {
    return "Moving " + before;
  }

  private static final Path basePath = FileSystems.getDefault().getPath("/workdir");

  static String validatePath(Location location, String strPath) throws EvalException {
    Path resolved = basePath.resolve(strPath);
    Path normalized = resolved.normalize();

    if (!resolved.toString().equals(normalized.toString()) || !normalized.startsWith(basePath)) {
      throw new EvalException(location,
          "'" + strPath + "' is not a relative path or contains unexpected ..");
    }
    return strPath;
  }
}
