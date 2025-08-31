package edu.unikom.herbamedjabar.di

import android.app.Application
import androidx.room.Room
import com.google.ai.client.generativeai.GenerativeModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.FirebaseStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import edu.unikom.herbamedjabar.R // Import R class
import edu.unikom.herbamedjabar.dao.ScanHistoryDao
import edu.unikom.herbamedjabar.db.AppDatabase
import edu.unikom.herbamedjabar.repository.PlantRepository
import edu.unikom.herbamedjabar.repository.PlantRepositoryImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore {
        return Firebase.firestore
    }

    @Provides
    @Singleton
    fun provideFirebaseStorage(): FirebaseStorage = FirebaseStorage.getInstance()

    @Provides
    @Singleton
    fun provideGenerativeModel(app: Application): GenerativeModel {
        val apiKey = app.getString(R.string.api_key)
        return GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = apiKey
        )
    }

    @Provides
    @Singleton
    fun provideAppDatabase(app: Application): AppDatabase {
        return Room.databaseBuilder(
            app,
            AppDatabase::class.java,
            "herb_app_db"
        ).addMigrations(AppDatabase.MIGRATION_1_2)
            .build()
    }

    @Provides
    @Singleton
    fun provideScanHistoryDao(db: AppDatabase): ScanHistoryDao {
        return db.scanHistoryDao()
    }

    @Provides
    @Singleton
    fun providePlantRepository(
        generativeModel: GenerativeModel,
        scanHistoryDao: ScanHistoryDao,
        app: Application
    ): PlantRepository {
        return PlantRepositoryImpl(generativeModel, scanHistoryDao, app)
    }
}
