
apply { plugin("kotlin") }

configureIntellijPlugin {
    setExtraDependencies("intellij-core")
}

val antLauncherJar by configurations.creating

dependencies {
    testCompile(protobufFull())
    testCompile(projectTests(":compiler:tests-common"))
    testCompileOnly(project(":compiler:frontend"))
    testCompileOnly(project(":compiler:cli"))
    testCompileOnly(project(":compiler:util"))
    testCompile(project(":js:js.translator"))
    testCompile(project(":js:js.serializer"))
    testCompile(project(":js:js.dce"))
    testCompile(commonDep("junit:junit"))
    testCompile(projectTests(":kotlin-build-common"))
    testCompile(projectTests(":generators:test-generator"))
    testRuntime(projectDist(":kotlin-compiler"))
    testRuntime(projectDist(":kotlin-stdlib"))
    testRuntime(projectDist(":kotlin-stdlib-js"))
    testRuntime(projectDist(":kotlin-test:kotlin-test-js")) // to be sure that kotlin-test-js built before tests runned
    testRuntime(projectDist(":kotlin-reflect"))
    testRuntime(projectDist(":kotlin-preloader")) // it's required for ant tests
    testRuntime(project(":compiler:backend-common"))
    testRuntime(commonDep("org.fusesource.jansi", "jansi"))

    antLauncherJar(commonDep("org.apache.ant", "ant"))
    antLauncherJar(files(toolsJar()))
}

afterEvaluate {
    dependencies {
        testCompileOnly(intellijCoreJar())
        testCompileOnly(intellij { include("openapi.jar", "idea.jar", "idea_rt.jar", "util.jar") })
        testRuntime(intellij())
    }
}


sourceSets {
    "main" {}
    "test" { projectDefault() }
}

val testDistProjects = listOf(
        "", // for root project
        ":prepare:mock-runtime-for-test",
        ":kotlin-compiler",
        ":kotlin-script-runtime",
        ":kotlin-stdlib",
        ":kotlin-daemon-client",
        ":kotlin-ant")

projectTest {
    dependsOn(*testDistProjects.map { "$it:dist" }.toTypedArray())
    workingDir = rootDir
    doFirst {
        systemProperty("ant.classpath", antLauncherJar.asPath)
        systemProperty("ant.launcher.class", "org.apache.tools.ant.Main")
    }
}

testsJar {}

projectTest("quickTest") {
    dependsOn(*testDistProjects.map { "$it:dist" }.toTypedArray())
    workingDir = rootDir
    systemProperty("kotlin.js.skipMinificationTest", "true")
    doFirst {
        systemProperty("ant.classpath", antLauncherJar.asPath)
        systemProperty("ant.launcher.class", "org.apache.tools.ant.Main")
    }
}

ant.importBuild(File(rootDir, "node_utils.xml"))

val prepareNode: Task = tasks["download-nodejs-and-npm"]
prepareNode.apply {
    outputs.upToDateWhen { File(rootDir, "dependencies/node").exists() }
}

projectTest("nodeTest") {
    workingDir = rootDir
    dependsOn(prepareNode)
    dependsOn("run-nodejs-tests")
}

val generateTests by generator("org.jetbrains.kotlin.generators.tests.GenerateJsTestsKt")
