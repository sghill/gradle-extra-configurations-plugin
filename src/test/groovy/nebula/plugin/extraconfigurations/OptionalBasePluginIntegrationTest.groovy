/*
 * Copyright 2014 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nebula.plugin.extraconfigurations

import nebula.test.IntegrationSpec

class OptionalBasePluginIntegrationTest extends IntegrationSpec {
    def "Publishing provided dependencies to a Maven repository preserves the scope"() {
        given:
        File repoUrl = new File(projectDir, 'build/repo')

        when:
        buildFile << """
apply plugin: 'java'
apply plugin: 'nebula-optional-base'
apply plugin: 'maven-publish'

group = 'nebula.extraconf'
version '1.0'

repositories {
    mavenCentral()
}

dependencies {
    compile 'org.apache.commons:commons-lang3:3.3.2', optional
}

publishing {
    publications {
        mavenJava(MavenPublication) {
            from components.java
        }
    }

    repositories {
        maven {
            url '$repoUrl.canonicalPath'
        }
    }
}
"""
        createProductionJavaSourceFile()
        runTasksSuccessfully('publish')

        then:
        File pomFile = new File(repoUrl, "nebula/extraconf/$moduleName/1.0/$moduleName-1.0.pom")
        pomFile.exists()
        def pomXml = new XmlSlurper().parseText(pomFile.text)
        def dependencies = pomXml.dependencies
        dependencies.size() == 1
        def commonsLang = dependencies.dependency[0]
        commonsLang.groupId.text() == 'org.apache.commons'
        commonsLang.artifactId.text() == 'commons-lang3'
        commonsLang.version.text() == '3.3.2'
        commonsLang.scope.text() == 'runtime'
        commonsLang.optional.text() == 'true'
    }

    def "Publishing optional dependencies to an Ivy repository preserves the scope"() {
        given:
        File repoUrl = new File(projectDir, 'build/repo')

        when:
        buildFile << """
apply plugin: 'java'
apply plugin: 'nebula-optional-base'
apply plugin: 'ivy-publish'

group = 'nebula.extraconf'
version '1.0'

repositories {
    mavenCentral()
}

dependencies {
    compile 'org.apache.commons:commons-lang3:3.3.2', optional
}

publishing {
    publications {
        ivyJava(IvyPublication) {
            from components.java
        }
    }

    repositories {
        ivy {
            url '$repoUrl.canonicalPath'
        }
    }
}
"""
        createProductionJavaSourceFile()
        runTasksSuccessfully('publish')

        then:
        true
        File ivyFile = new File(repoUrl, "nebula.extraconf/$moduleName/1.0/ivy-1.0.xml")
        ivyFile.exists()
        def ivyXml = new XmlSlurper().parseText(ivyFile.text)
        def dependencies = ivyXml.dependencies
        dependencies.size() == 1
        def commonsLang = dependencies.dependency[0]
        commonsLang.@org.text() == 'org.apache.commons'
        commonsLang.@name.text() == 'commons-lang3'
        commonsLang.@rev.text() == '3.3.2'
        commonsLang.@conf.text() == 'optional'
    }

    private void createProductionJavaSourceFile() {
        File javaFile = createFile("src/main/java/nebula/extraconf/HelloWorld.java")
        javaFile << """
package nebula.extraconf;

import org.apache.commons.lang3.StringUtils;

public class HelloWorld {
    public String getMessage() {
        return StringUtils.upperCase("Hello World!");
    }
}
"""
    }
}
