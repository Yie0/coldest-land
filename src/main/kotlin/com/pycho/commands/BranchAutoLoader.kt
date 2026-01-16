package com.pycho.commands

import java.io.File
import java.net.URL
import java.util.jar.JarFile

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class PychoCommand

object CommandAutoLoader {
    private data class ScanResult(
        val loaded: List<String>,
        val skipped: List<String>,
        val errors: List<String>
    )

    fun loadPackage(packageName: String) {
        val result = scanPackage(packageName)
        printReport(packageName, result)
    }

    private fun scanPackage(packageName: String): ScanResult {
        val packagePath = packageName.replace('.', '/')
        val classLoader = Thread.currentThread().contextClassLoader ?: ClassLoader.getSystemClassLoader()
        val resources = classLoader.getResources(packagePath).toList()

        val loaded = mutableListOf<String>()
        val skipped = mutableListOf<String>()
        val errors = mutableListOf<String>()

        resources.forEach { url ->
            runCatching {
                val classNames = when (url.protocol) {
                    "file" -> scanFileSystem(url, packagePath, packageName)
                    "jar" -> scanJarFile(url, packagePath)
                    else -> emptyList()
                }

                classNames.forEach { className ->
                    when (loadClassIfAnnotated(className)) {
                        LoadResult.Loaded -> loaded.add(className)
                        LoadResult.Skipped -> skipped.add(className)
                        is LoadResult.Error -> errors.add(className)
                    }
                }
            }.onFailure { throwable ->
                errors.add("Failed to scan $url: ${throwable.message}")
            }
        }

        return ScanResult(loaded, skipped, errors)
    }

    private fun scanFileSystem(url: URL, packagePath: String, packageName: String): List<String> {
        val dir = File(url.toURI())
        if (!dir.exists() || !dir.isDirectory) return emptyList()

        return dir.walkTopDown()
            .filter { it.isFile && it.extension == "class" }
            .map { file ->
                val relativePath = file.relativeTo(dir).path.replace(File.separatorChar, '/')
                "$packageName.${relativePath.removeSuffix(".class")}".replace('/', '.')
            }
            .toList()
    }

    private fun scanJarFile(url: URL, packagePath: String): List<String> {
        val jarPath = url.path.substringBefore("!").removePrefix("file:")
        val jarFile = JarFile(jarPath)

        return jarFile.entries().toList()
            .filter { it.name.startsWith(packagePath) && it.name.endsWith(".class") }
            .map { it.name.removeSuffix(".class").replace('/', '.') }
    }

    private sealed class LoadResult {
        object Loaded : LoadResult()
        object Skipped : LoadResult()
        data class Error(val message: String) : LoadResult()
    }

    private fun loadClassIfAnnotated(className: String): LoadResult {
        return runCatching {
            val clazz = Class.forName(className, false, Thread.currentThread().contextClassLoader)
            if (clazz.isAnnotationPresent(PychoCommand::class.java)) {
                Class.forName(className)
                LoadResult.Loaded
            } else {
                LoadResult.Skipped
            }
        }.getOrElse { throwable ->
            when (throwable) {
                is ClassNotFoundException, is NoClassDefFoundError -> LoadResult.Skipped
                else -> LoadResult.Error(throwable.message ?: "Unknown error")
            }
        }
    }

    private fun printReport(packageName: String, result: ScanResult) {
        println("\n[CommandAutoLoader] Package: $packageName")
        println("[CommandAutoLoader] ✓ Loaded: ${result.loaded.size} commands")
        println("[CommandAutoLoader] ⊘ Skipped: ${result.skipped.size} classes")
        println("[CommandAutoLoader] ✗ Errors: ${result.errors.size}")

        if (result.loaded.isNotEmpty()) {
            println("\n[CommandAutoLoader] Loaded commands:")
            result.loaded.forEach { println("  • $it") }
        }

        if (result.errors.isNotEmpty()) {
            println("\n[CommandAutoLoader] Errors:")
            result.errors.take(5).forEach { println("  ✗ $it") }
            if (result.errors.size > 5) {
                println("  ... and ${result.errors.size - 5} more")
            }
        }
    }
}