package dev.sendiko.bubblesync.di

import dev.sendiko.bubblesync.core.utils.HelloService
import io.ktor.server.application.*
import io.minio.MinioClient
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun Application.configureKoinModules() {
    val config = environment.config
    install(Koin) {
        slf4jLogger()
        modules(module {
            single {
                MinioClient.builder()
                    .endpoint(System.getenv("MINIO_ENDPOINT") ?: config.property("storage.endpoint").getString())
                    .credentials(
                        System.getenv("MINIO_ACCESS_KEY") ?: config.property("storage.accessKey").getString(),
                        System.getenv("MINIO_SECRET_KEY") ?: config.property("storage.secretKey").getString(),
                    )
                    .build()
            }
            single<HelloService> {
                HelloService {
                    println(environment.log.info("Hello, World!"))
                }
            }
        })
    }
}