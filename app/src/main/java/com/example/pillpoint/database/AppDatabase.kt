package com.example.pillpoint.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        User::class,
        Medication::class,
        CartEntity::class,
        OrderEntity::class,
        Prescription::class,
        Reminder::class,
        StoreLocation::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun appDao(): AppDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pillpoint_database"
                )
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        seedDatabase(database.appDao())
                    }
                }
            }

            suspend fun seedDatabase(dao: AppDao) {
                // 1. Seed Users (10 Records)
                val users = (1..10).map { i ->
                    User(
                        fullName = "Patient User $i",
                        email = "user$i@pillpoint.co.za",
                        passwordHash = "hashed_password_$i",
                        phone = "+2771000000$i",
                        deliveryAddress = "$i Main Street, Johannesburg"
                    )
                }
                dao.insertUsers(users)

                // 2. Seed Medications (10 Records)
                val medications = listOf(
                    Medication("1", "Allergex Syrup", "Allergy", 18.99, "Relieves allergic reactions", "allergexsyrup"),
                    Medication("2", "Allergex Tablets", "Allergy", 25.99, "Antihistamine tablets", "allergextablets"),
                    Medication("3", "Andolex Oral Gel", "Oral Care", 25.99, "Mouth ulcer pain relief", "andolexcoralgel"),
                    Medication("4", "Band-Aid Plastic", "First Aid", 39.99, "Sterile adhesive bandages", "bandaidplastic"),
                    Medication("5", "Benelyn Syrup", "Cough & Cold", 79.99, "Relieves chesty coughs", "benelyinsyrup"),
                    Medication("6", "Berroca", "Vitamins", 120.00, "Effervescent multivitamin", "berocca"),
                    Medication("7", "Calpol Syrup", "Pain & Fever", 67.99, "Pain relief for children", "calpol"),
                    Medication("8", "Canvex V", "Personal Care", 89.00, "Antifungal treatment", "canvexv"),
                    Medication("9", "Compral Tablets", "Pain Relief", 83.99, "Headache and fever relief", "compral"),
                    Medication("10", "Coryx", "Cough & Cold", 72.99, "Nasal decongestant", "coryx")
                )
                dao.insertMedications(medications)

                // 3. Seed Orders (10 Records)
                val orders = (1..10).map { i ->
                    OrderEntity(
                        userEmail = "user$i@pillpoint.co.za",
                        itemsSummary = "Allergex Syrup x1, Panado Tablets x1",
                        totalPrice = 56.98 + (i * 10),
                        paymentMethod = "Credit Card",
                        status = if (i % 2 == 0) "Delivered" else "Out for Delivery",
                        deliveryAddress = "$i Main Street, Johannesburg",
                        orderDate = "2026-10-0$i"
                    )
                }
                dao.insertOrders(orders)

                // 4. Seed Prescriptions (10 Records)
                val prescriptions = (1..10).map { i ->
                    Prescription(
                        userEmail = "user$i@pillpoint.co.za",
                        doctorName = "Dr. Smith #$i",
                        medicationName = "Amoxicillin 500mg",
                        dosage = "1 capsule 3 times daily",
                        scriptUrlOrPath = "scripts/script_$i.pdf",
                        status = if (i % 2 == 0) "Approved" else "Pending Verification"
                    )
                }
                dao.insertPrescriptions(prescriptions)

                // 5. Seed Reminders (10 Records)
                val reminders = (1..10).map { i ->
                    Reminder(
                        userEmail = "user$i@pillpoint.co.za",
                        medicineName = "Medication $i",
                        reminderTime = "${7 + (i % 4)}:00 AM",
                        dosageInfo = "Take 1 tablet after food",
                        isActive = true
                    )
                }
                dao.insertReminders(reminders)

                // 6. Seed Store Locations (10 Records)
                val stores = listOf(
                    StoreLocation(storeName = "Pill Point Rosebank", address = "Oxford Rd, Rosebank", phone = "011 234 5671", operatingHours = "08:00 - 18:00", latitude = -26.146, longitude = 28.043),
                    StoreLocation(storeName = "Pill Point Sandton", address = "Sandton City, Sandton", phone = "011 234 5672", operatingHours = "08:00 - 19:00", latitude = -26.107, longitude = 28.056),
                    StoreLocation(storeName = "Pill Point Braamfontein", address = "Juta St, Braamfontein", phone = "011 234 5673", operatingHours = "08:00 - 17:00", latitude = -26.192, longitude = 28.034),
                    StoreLocation(storeName = "Pill Point Pretoria Central", address = "Church St, Pretoria", phone = "012 345 6781", operatingHours = "08:00 - 18:00", latitude = -25.746, longitude = 28.188),
                    StoreLocation(storeName = "Pill Point Centurion", address = "Heuwel Ave, Centurion", phone = "012 345 6782", operatingHours = "08:00 - 18:00", latitude = -25.860, longitude = 28.189),
                    StoreLocation(storeName = "Pill Point Midrand", address = "Harry Galaun Dr, Midrand", phone = "011 315 1234", operatingHours = "08:00 - 18:00", latitude = -25.996, longitude = 28.127),
                    StoreLocation(storeName = "Pill Point Soweto", address = "Vilakazi St, Soweto", phone = "011 982 4321", operatingHours = "08:00 - 17:00", latitude = -26.238, longitude = 27.908),
                    StoreLocation(storeName = "Pill Point Benoni", address = "Tom Jones St, Benoni", phone = "011 421 9876", operatingHours = "08:00 - 17:00", latitude = -26.188, longitude = 28.320),
                    StoreLocation(storeName = "Pill Point Randburg", address = "Malibongwe Dr, Randburg", phone = "011 789 6543", operatingHours = "08:00 - 18:00", latitude = -26.093, longitude = 27.984),
                    StoreLocation(storeName = "Pill Point Springs", address = "4th Ave, Springs", phone = "011 812 3456", operatingHours = "08:00 - 17:00", latitude = -26.250, longitude = 28.442)
                )
                dao.insertStoreLocations(stores)
            }
        }
    }
}