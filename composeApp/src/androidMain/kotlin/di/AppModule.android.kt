package di

import org.koin.core.module.Module
import org.koin.dsl.module
import domain.repository.StickerRepository

actual fun platformModule(): Module = module {
    // TODO: Implement actual repository for Android
    // single<StickerRepository> { StickerRepositoryImpl(androidContext()) }
}
