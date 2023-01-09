package com.savent.erp

import com.savent.erp.data.local.datasource.DataObjectStorage
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.savent.erp.data.local.database.AppDatabase
import com.savent.erp.data.local.datasource.*
import com.savent.erp.data.local.model.AppPreferences
import com.savent.erp.data.local.model.BusinessBasicsLocal
import com.savent.erp.data.local.model.SaleEntity
import com.savent.erp.data.remote.datasource.*
import com.savent.erp.data.remote.service.*
import com.savent.erp.data.repository.*
import com.savent.erp.domain.repository.*
import com.savent.erp.domain.usecase.*
import com.savent.erp.presentation.viewmodel.*
import com.savent.erp.data.remote.model.LoginCredentials
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import javax.security.cert.CertificateException

private val Context.datastore: DataStore<Preferences> by preferencesDataStore(AppConstants.APP_PREFERENCES)

val baseModule = module {

    single { Gson() }

    /*single<OkHttpClient> {

        OkHttpClient.Builder().addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("Authorization", AppConstants.AUTHORIZATION)
                .build()
            chain.proceed(request)
        }.build()
    }*/

    fun getUnsafeOkHttpClient(): OkHttpClient.Builder {
        return try {
            // Create a trust manager that does not validate certificate chains
            val trustAllCerts: Array<TrustManager> = arrayOf<TrustManager>(
                object : X509TrustManager {
                    @Throws(CertificateException::class)
                    override fun checkClientTrusted(
                        chain: Array<X509Certificate?>?,
                        authType: String?
                    ) {
                    }

                    @Throws(CertificateException::class)
                    override fun checkServerTrusted(
                        chain: Array<X509Certificate?>?,
                        authType: String?
                    ) {
                    }

                    override fun getAcceptedIssuers(): Array<X509Certificate> {
                        return arrayOf()
                    }

                }
            )

            // Install the all-trusting trust manager
            val sslContext: SSLContext = SSLContext.getInstance("SSL")
            sslContext.init(null, trustAllCerts, SecureRandom())

            // Create an ssl socket factory with our all-trusting manager
            val sslSocketFactory: SSLSocketFactory = sslContext.getSocketFactory()
            val builder = OkHttpClient.Builder()
            builder.sslSocketFactory(sslSocketFactory, trustAllCerts[0] as X509TrustManager)
            builder.hostnameVerifier { _, _ -> true }
            builder.addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("Authorization", AppConstants.AUTHORIZATION)
                    .build()
                chain.proceed(request)
            }
            builder
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    single<OkHttpClient> {
        getUnsafeOkHttpClient().build()
    }

    single {
        Room.databaseBuilder(
            androidApplication(),
            AppDatabase::class.java,
            AppConstants.APP_DATABASE_NAME
        ).build()
    }

    single {
        Retrofit.Builder()
            .baseUrl(AppConstants.SAVENT_POS_API_BASE_URL)
            .client(get())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    single<AppPreferencesLocalDatasource> {
        AppPreferencesLocalDatasourceImpl(
            DataObjectStorage<AppPreferences>(
                get(),
                object : TypeToken<AppPreferences>() {}.type,
                androidContext().datastore,
                stringPreferencesKey((AppConstants.APP_PREFERENCES))
            )
        )
    }
}


val businessBasicsDataModule = module {

    includes(baseModule)


    single<BusinessBasicsLocalDatasource> {
        BusinessBasicsLocalDatasourceImpl(
            DataObjectStorage<BusinessBasicsLocal>(
                get(),
                object : TypeToken<BusinessBasicsLocal>() {}.type,
                androidContext().datastore,
                stringPreferencesKey((AppConstants.BUSINESS_PREFERENCES))
            )
        )
    }

    single<BusinessBasicsApiService> {
        get<Retrofit>().create(BusinessBasicsApiService::class.java)
    }

    single<BusinessBasicsRemoteDatasource> {
        BusinessBasicsRemoteDatasourceImpl(get())
    }

    /*single<BusinessBasicsRemoteDatasource> {
        BusinessBasicsRemoteDatasourceFake()
    }*/

    single<BusinessBasicsRepository> {
        BusinessBasicsRepositoryImpl(get(), get())
    }

    single {
        RemoteBusinessBasicsSyncFromLocalUseCase(get(), get())
    }

}

val salesDataModule = module {

    includes(baseModule)

    single {
        get<AppDatabase>().saleDao()
    }

    single<PendingSaleLocalDatasource> {
        PendingSaleLocalDatasourceImpl(
            DataObjectStorage<SaleEntity>(
                get(), object : TypeToken<SaleEntity>() {}.type, androidContext().datastore,
                stringPreferencesKey((AppConstants.PENDING_SALE_PREFERENCES))
            )
        )
    }

    single<SalesLocalDatasource> {
        SalesLocalDatasourceImpl(get())
    }

    single<SaleApiService> {
        get<Retrofit>().create(SaleApiService::class.java)
    }

    single<SalesRemoteDatasource> {
        SalesRemoteDatasourceImpl(get())
    }

    single<SalesRepository> {
        SalesRepositoryImpl(get(), get(), get())
    }

}

val debtPaymentsDataModule = module {

    includes(baseModule)

    single {
        get<AppDatabase>().debtPaymentDao()
    }

    single<DebtPaymentLocalDatasource> {
        DebtPaymentLocalDatasourceImpl(get())
    }

    single<DebtPaymentApiService> {
        get<Retrofit>().create(DebtPaymentApiService::class.java)
    }

    single<DebtPaymentRemoteDatasource> {
        DebtPaymentRemoteDatasourceImpl(get())
    }

    single<DebtPaymentRepository> {
        DebtPaymentRepositoryImpl(get())
    }

    single {
        RemoteDebtPaymentSyncFromLocalUseCase(get(), get())
    }

}

val incompletePaymentsDataModule = module {

    includes(baseModule)

    single {
        get<AppDatabase>().incompletePaymentDao()
    }

    single<IncompletePaymentsLocalDataSource> {
        IncompletePaymentsLocalDatasourceImpl(get())
    }

    single<IncompletePaymentApiService> {
        get<Retrofit>().create(IncompletePaymentApiService::class.java)
    }

    single<IncompletePaymentsRemoteDatasource> {
        IncompletePaymentsRemoteDatasourceImpl(get())
    }

    single<IncompletePaymentRepository> {
        IncompletePaymentRepositoryImpl(get(), get())
    }

}

val productsDataModule = module {

    includes(baseModule)

    single {
        get<AppDatabase>().productDao()
    }

    single<ProductsLocalDatasource> {
        ProductsLocalDatasourceImpl(get())
    }

    single<ProductApiService> {
        get<Retrofit>().create(ProductApiService::class.java)
    }

    single<ProductsRemoteDatasource> {
        ProductsRemoteDatasourceImpl(get())
    }

    /*single<ProductsRemoteDatasource> {
        ProductsRemoteDataSourceFake()
    }*/

    single<ProductsRepository> {
        ProductsRepositoryImpl(get(), get())
    }

}

val clientsDataModule = module {

    includes(baseModule)

    single {
        get<AppDatabase>().clientDao()
    }

    single<ClientsLocalDatasource> {
        ClientsLocalDatasourceImpl(get())
    }

    single<ClientApiService> {
        get<Retrofit>().create(ClientApiService::class.java)
    }

    single<ClientsRemoteDatasource> {
        ClientsRemoteDatasourceImpl(get())
    }

    /*single<ClientsRemoteDatasource> {
        ClientsRemoteDatasourceFake()
    }*/

    single<ClientsRepository> {
        ClientsRepositoryImpl(get(), get())
    }

}

val statsDataModule = module {

    includes(baseModule)

    single {
        get<AppDatabase>().statDao()
    }

    single<StatsLocalDatasource> {
        StatsLocalDataSourceImpl(get())
    }

    single<StatApiService> {
        get<Retrofit>().create(StatApiService::class.java)
    }

    single<StatsRemoteDatasource> {
        StatsRemoteDatasourceImpl(get())
    }

    single<StatsRepository> {
        StatsRepositoryImpl(get(), get())
    }

}

val businessDataModule = module {

    includes(
        baseModule,
        businessBasicsDataModule,
        salesDataModule,
        clientsDataModule,
        productsDataModule,
        incompletePaymentsDataModule,
        debtPaymentsDataModule,
        statsDataModule
    )

    single<BusinessApiService> {
        get<Retrofit>().create(BusinessApiService::class.java)
    }

    single<BusinessRemoteDatasource> {
        BusinessRemoteDatasourceImpl(get())
    }

    single<BusinessRepository> {
        BusinessRepositoryImpl(get(), get(), get(), get(), get())
    }

}

val credentialsDataModule = module {

    includes(baseModule)

    single<CredentialsLocalDatasource> {
        CredentialsLocalDatasourceImpl(
            DataObjectStorage<LoginCredentials>(
                get(), object : TypeToken<LoginCredentials>() {}.type, androidContext().datastore,
                stringPreferencesKey((AppConstants.LOGIN_CREDENTIALS))
            )
        )
    }

    single<CredentialsRepository> {
        CredentialsRepositoryImpl(get())
    }
}

val dataModule = module {
    includes(businessDataModule, credentialsDataModule)
}

val openingModule = module {

    includes(dataModule)

    single {
        IsLoggedUseCase(get())
    }

    viewModel { OpeningViewModel(get()) }

}

val loginModule = module {

    includes(dataModule)

    single {
        LoginUseCase(get(), get())
    }

    viewModel { LoginViewModel(androidApplication(), get(), get()) }

}

val dashboardModule = module {

    includes(dataModule)

    single {
        ReloadBusinessBasicsDataUseCase(get())
    }

    single {
        ReloadStatsDataUseCase(get())
    }

    single {
        GetBusinessBasicsUseCase(get())
    }

    viewModel { DashboardViewModel(androidApplication(), get(), get(), get()) }

}

val saleModule = module {

    includes(dataModule)

    viewModel { MainViewModel(androidApplication()) }


    single {
        CreatePendingSaleUseCase(get())
    }

    single {
        GetPendingSaleUseCase(get())
    }

    single {
        IncreaseExtraDiscountPercentUseCase(get(), get())
    }

    single {
        DecreaseExtraDiscountPercentUseCase(get(), get())
    }

    single {
        UpdateExtraDiscountPercentUseCase(get(), get())
    }

    single {
        UpdateCollectedPaymentUseCase(get())
    }

    single {
        UpdatePaymentMethodUseCase(get())
    }

    single {
        GetSalesOfDayUseCase(get())
    }

    single {
        ComputeBalanceUseCase(get())
    }

    single {
        RemoteSaleSyncFromLocalUseCase(get(), get(), get(), get())
    }

    viewModel { SalesViewModel(get(), get()) }
}

val clientsModule = module {

    includes(saleModule)

    single {
        GetClientListUseCase(get(), get())
    }

    single {
        ReloadClientsUseCase(get())
    }

    single {
        RemoveAllClientsUseCase(get())
    }

    single {
        CreateNewClientUseCase(get())
    }

    single {
        AddClientToSaleUseCase(get(), get())
    }

    single {
        ValidateClientUseCase()
    }

    single {
        RemoteClientSyncFromLocalUseCase(get(), get())
    }

    viewModel {
        ClientsViewModel(
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get()
        )
    }

}

val productsModule = module {

    includes(dataModule)

    single {
        GetProductListUseCase(get(), get())
    }

    single {
        ReloadProductsUseCase(get())
    }

    single {
        RemoveAllProductsUseCase(get())
    }

    single {
        ComputePendingSalePriceUseCase(get(), get())
    }

    single {
        AddProductToSaleUseCase(get(), get())
    }

    single {
        RemoveProductFromSaleUseCase(get(), get())
    }

    single {
        ChangeUnitsOfSelectedProductsUseCase(get(), get())
    }

    single {
        RemoteProductSyncFromLocalUseCase(get(), get())
    }

    viewModel {
        ProductsViewModel(
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get()
        )
    }

}

val checkoutModule = module {

    includes(productsModule, saleModule)

    single {
        GetCheckoutSaleUseCase(get(), get())
    }

    single {
        GetSelectedProductsUseCase(get(), get())
    }

    single {
        SaveCompletedSaleUseCase(get(), get(), get())
    }

    single {
        GetReceiptToSend(get(), get(), get(), get())
    }

    single {
        GetReceiptToPrint(get(), get(), get())
    }

    viewModel {
        CheckoutViewModel(
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get()
        )
    }

}

val debtsModule = module {
    includes(debtPaymentsDataModule, incompletePaymentsDataModule, clientsModule)

    single {
        GetClientListWithDebts(get(), get(), get())
    }

    single {
        GetIncompletePaymentsUseCase(get())
    }

    single {
        ReloadIncompletePaymentsUseCase(get())
    }

    single {
        PayDebtUseCase(get(),get())
    }

    viewModel { DebtsViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }

}

val appModule = module {
    includes(openingModule, loginModule, dashboardModule, checkoutModule, debtsModule)

    /*single<ConnectivityObserver> {
        NetworkConnectivityObserver(androidContext())
    }*/

    single {
        RemoteDataSyncFromLocalUseCase(get(), get(), get(), get(), get())
    }
}


