/*
 * Copyright (C) 2023 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.copybara.util;

import static com.google.common.truth.Truth.assertThat;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.hash.Hashing;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public final class SinglePatchUtilTest {
  @Rule
  public final TemporaryFolder tmpFolder = new TemporaryFolder();
  private Path workdir;
  private Path baseline;
  private Path destination;

  @Before
  public void setUp() throws IOException {
    workdir = tmpFolder.getRoot().toPath();
    baseline = Files.createDirectories(workdir.resolve("baseline"));
    destination = Files.createDirectories(workdir.resolve("destination"));
  }

  @Test
  public void testGenerateSinglePatch() throws IOException {
    SinglePatch singlePatch = SinglePatchUtil.generateSinglePatch(destination, baseline,
        Hashing.sha256());
    assertThat(singlePatch).isNotNull();
  }

  @Test
  public void testGenerateSinglePatchHashesFile() throws IOException {

    String testPath = "test/foo";
    String testContents = "hello";
    byte[] testBytes = testContents.getBytes(UTF_8);

    Files.createDirectories(destination.resolve(testPath).getParent());
    Files.write(destination.resolve(testPath), testBytes);

    SinglePatch singlePatch = SinglePatchUtil.generateSinglePatch(destination, baseline,
        Hashing.sha256());

    assertThat(singlePatch.getFileHashes()).containsExactly(
        testPath, new String(Hashing.sha256().hashBytes(testBytes).asBytes(), UTF_8)
    );
  }

}
