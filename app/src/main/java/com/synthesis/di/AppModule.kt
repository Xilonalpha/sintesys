package com.synthesis.di

import android.content.Context
import androidx.room.Room
import com.synthesis.data.local.SynthesisDatabase
import com.synthesis.data.remote.api.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SynthesisDatabase {
        return Room.databaseBuilder(context, SynthesisDatabase::class.java, "synthesis_database")
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides @Singleton
    fun provideXilonDiscoveryDao(database: SynthesisDatabase): com.synthesis.data.local.dao.XilonDiscoveryDao =
        database.xilonDiscoveryDao()

    @Provides @Singleton
    fun provideCachedExoplanetDao(database: SynthesisDatabase): com.synthesis.data.local.dao.CachedExoplanetDao =
        database.cachedExoplanetDao()

    @Provides @Singleton
    fun provideSynthesizedResearchDao(database: SynthesisDatabase): com.synthesis.data.local.dao.SynthesizedResearchDao =
        database.synthesizedResearchDao()

    @Provides @Singleton
    fun provideDiscoveryDao(database: SynthesisDatabase): com.synthesis.data.local.dao.DiscoveryDao =
        database.discoveryDao()

    @Provides @Singleton
    fun providePureTicDao(database: SynthesisDatabase): com.synthesis.data.local.dao.PureTicDao =
        database.pureTicDao()

    @Provides @Singleton
    fun provideOkHttpClient(@ApplicationContext context: Context): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
        val cacheSize = 50L * 1024 * 1024 // 50 MB HTTP Cache
        val cache = okhttp3.Cache(java.io.File(context.cacheDir, "http_cache"), cacheSize)
        return OkHttpClient.Builder()
            .cache(cache)
            .addInterceptor(logging)
            .connectTimeout(35, TimeUnit.SECONDS)
            .readTimeout(35, TimeUnit.SECONDS)
            .build()
    }

    // NASA (api.nasa.gov)
    @Provides @Singleton @NasaRetrofit
    fun provideNasaRetrofit(client: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl("https://api.nasa.gov/").client(client).addConverterFactory(GsonConverterFactory.create()).build()
    @Provides @Singleton fun provideNasaApi(@NasaRetrofit retrofit: Retrofit): NasaApi = retrofit.create(NasaApi::class.java)

    // NASA POWER (power.larc.nasa.gov)
    @Provides @Singleton @NasaPowerRetrofit
    fun provideNasaPowerRetrofit(client: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl("https://power.larc.nasa.gov/").client(client).addConverterFactory(GsonConverterFactory.create()).build()
    @Provides @Singleton fun provideNasaPowerApi(@NasaPowerRetrofit retrofit: Retrofit): NasaPowerApi = retrofit.create(NasaPowerApi::class.java)

    // USGS (earthquake.usgs.gov)
    @Provides @Singleton @UsgsRetrofit
    fun provideUsgsRetrofit(client: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl("https://earthquake.usgs.gov/").client(client).addConverterFactory(GsonConverterFactory.create()).build()
    @Provides @Singleton fun provideUsgsApi(@UsgsRetrofit retrofit: Retrofit): UsgsApi = retrofit.create(UsgsApi::class.java)

    // Open-Meteo (api.open-meteo.com)
    @Provides @Singleton @OpenMeteoRetrofit
    fun provideOpenMeteoRetrofit(client: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl("https://api.open-meteo.com/").client(client).addConverterFactory(GsonConverterFactory.create()).build()
    @Provides @Singleton fun provideOpenMeteoApi(@OpenMeteoRetrofit retrofit: Retrofit): OpenMeteoApi = retrofit.create(OpenMeteoApi::class.java)

    // PubMed (eutils.ncbi.nlm.nih.gov)
    @Provides @Singleton @PubMedRetrofit
    fun providePubMedRetrofit(client: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl("https://eutils.ncbi.nlm.nih.gov/entrez/eutils/").client(client).addConverterFactory(GsonConverterFactory.create()).build()
    @Provides @Singleton fun providePubMedApi(@PubMedRetrofit retrofit: Retrofit): PubMedApi = retrofit.create(PubMedApi::class.java)

    // NOAA / NWS (api.weather.gov)
    @Provides @Singleton @NoaaRetrofit
    fun provideNoaaRetrofit(client: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl("https://api.weather.gov/").client(client).addConverterFactory(GsonConverterFactory.create()).build()
    @Provides @Singleton fun provideNoaaApi(@NoaaRetrofit retrofit: Retrofit): NoaaApi = retrofit.create(NoaaApi::class.java)

    // arXiv (export.arxiv.org)
    @Provides @Singleton @ArxivRetrofit
    fun provideArxivRetrofit(client: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl("http://export.arxiv.org/").client(client).addConverterFactory(ScalarsConverterFactory.create()).build()
    @Provides @Singleton fun provideArxivApi(@ArxivRetrofit retrofit: Retrofit): ArxivApi = retrofit.create(ArxivApi::class.java)

    // ESA Gaia (gaia.aip.de)
    @Provides @Singleton @GaiaRetrofit
    fun provideGaiaRetrofit(client: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl("https://gaia.aip.de/").client(client).addConverterFactory(GsonConverterFactory.create()).build()
    @Provides @Singleton fun provideGaiaApi(@GaiaRetrofit retrofit: Retrofit): GaiaApi = retrofit.create(GaiaApi::class.java)

    // CERN Open Data (opendata.cern.ch)
    @Provides @Singleton @CernRetrofit
    fun provideCernRetrofit(client: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl("https://opendata.cern.ch/").client(client).addConverterFactory(GsonConverterFactory.create()).build()
    @Provides @Singleton fun provideCernApi(@CernRetrofit retrofit: Retrofit): CernApi = retrofit.create(CernApi::class.java)

    // Copernicus Data Space (catalogue.dataspace.copernicus.eu)
    @Provides @Singleton @CopernicusRetrofit
    fun provideCopernicusRetrofit(client: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl("https://catalogue.dataspace.copernicus.eu/").client(client).addConverterFactory(GsonConverterFactory.create()).build()
    @Provides @Singleton fun provideCopernicusApi(@CopernicusRetrofit retrofit: Retrofit): CopernicusApi = retrofit.create(CopernicusApi::class.java)

    // GBIF (api.gbif.org)
    @Provides @Singleton @GbifRetrofit
    fun provideGbifRetrofit(client: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl("https://api.gbif.org/").client(client).addConverterFactory(GsonConverterFactory.create()).build()
    @Provides @Singleton fun provideGbifApi(@GbifRetrofit retrofit: Retrofit): GbifApi = retrofit.create(GbifApi::class.java)

    // ESA Sentinel Hub (services.sentinel-hub.com)
    @Provides @Singleton @SentinelRetrofit
    fun provideSentinelRetrofit(client: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl("https://services.sentinel-hub.com/").client(client).addConverterFactory(GsonConverterFactory.create()).build()
    @Provides @Singleton fun provideSentinelApi(@SentinelRetrofit retrofit: Retrofit): SentinelApi = retrofit.create(SentinelApi::class.java)

    // NCBI Virus / Datasets (api.ncbi.nlm.nih.gov)
    @Provides @Singleton @NcbiVirusRetrofit
    fun provideNcbiVirusRetrofit(client: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl("https://api.ncbi.nlm.nih.gov/").client(client).addConverterFactory(GsonConverterFactory.create()).build()
    @Provides @Singleton fun provideNcbiVirusApi(@NcbiVirusRetrofit retrofit: Retrofit): NcbiVirusApi = retrofit.create(NcbiVirusApi::class.java)

    // World Bank (api.worldbank.org)
    @Provides @Singleton @WorldBankRetrofit
    fun provideWorldBankRetrofit(client: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl("https://api.worldbank.org/").client(client).addConverterFactory(GsonConverterFactory.create()).build()
    @Provides @Singleton fun provideWorldBankApi(@WorldBankRetrofit retrofit: Retrofit): WorldBankApi = retrofit.create(WorldBankApi::class.java)

    // JPL Horizons (ssd.jpl.nasa.gov)
    @Provides @Singleton @JplHorizonsRetrofit
    fun provideJplHorizonsRetrofit(client: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl("https://ssd.jpl.nasa.gov/").client(client).addConverterFactory(ScalarsConverterFactory.create()).build()
    @Provides @Singleton fun provideJplHorizonsApi(@JplHorizonsRetrofit retrofit: Retrofit): JplHorizonsApi = retrofit.create(JplHorizonsApi::class.java)

    // NASA Exoplanet Archive TAP service (exoplanetarchive.ipac.caltech.edu)
    @Provides @Singleton @ExoplanetArchiveRetrofit
    fun provideExoplanetArchiveRetrofit(client: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl("https://exoplanetarchive.ipac.caltech.edu/").client(client).addConverterFactory(GsonConverterFactory.create()).build()
    @Provides @Singleton fun provideExoplanetArchiveApi(@ExoplanetArchiveRetrofit retrofit: Retrofit): ExoplanetArchiveApi = retrofit.create(ExoplanetArchiveApi::class.java)

    // STScI MAST Exo Data Validation API (exo.mast.stsci.edu)
    @Provides @Singleton @MastExoRetrofit
    fun provideMastExoRetrofit(client: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl("https://exo.mast.stsci.edu/").client(client).addConverterFactory(GsonConverterFactory.create()).build()
    @Provides @Singleton fun provideMastExoApi(@MastExoRetrofit retrofit: Retrofit): MastExoApi = retrofit.create(MastExoApi::class.java)

    // STScI MAST Portal Catalog API (mast.stsci.edu)
    @Provides @Singleton @MastPortalRetrofit
    fun provideMastPortalRetrofit(client: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl("https://mast.stsci.edu/").client(client).addConverterFactory(GsonConverterFactory.create()).build()
    @Provides @Singleton fun provideMastPortalApi(@MastPortalRetrofit retrofit: Retrofit): MastPortalApi = retrofit.create(MastPortalApi::class.java)

    // Google Play Billing
    @Provides
    @Singleton
    fun providePlayBillingService(@ApplicationContext context: Context): com.synthesis.data.billing.PlayBillingService {
        return com.synthesis.data.billing.PlayBillingService(context)
    }
}
