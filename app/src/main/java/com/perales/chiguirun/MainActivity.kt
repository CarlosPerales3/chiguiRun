package com.perales.chiguirun

import android.Manifest
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.media.MediaPlayer
import android.provider.Settings
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.NumberPicker
import android.widget.RelativeLayout
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import com.facebook.login.LoginManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.gms.maps.model.RoundCap
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.perales.chiguirun.Constants.INTERVAL_LOCATION
import com.perales.chiguirun.Constants.LIMIT_DISTANCE_ACCEPTED_RUNNING
import com.perales.chiguirun.LoginActivity.Companion.providerSession
import com.perales.chiguirun.LoginActivity.Companion.userEmail
import com.perales.chiguirun.Utility.animateViewofFloat
import com.perales.chiguirun.Utility.animateViewofInt
import com.perales.chiguirun.Utility.deleteRunAndLinkedData
import com.perales.chiguirun.Utility.getFormattedStopWatch
import com.perales.chiguirun.Utility.getFormattedTotalTime
import com.perales.chiguirun.Utility.getSecFromWatch
import com.perales.chiguirun.Utility.roundNumber
import com.perales.chiguirun.Utility.setHeightLinearLayout
import me.tankery.lib.circularseekbar.CircularSeekBar
import java.text.SimpleDateFormat
import java.util.Date


class MainActivity : AppCompatActivity(),
    NavigationView.OnNavigationItemSelectedListener,
    OnMapReadyCallback,
    GoogleMap.OnMyLocationClickListener,
    GoogleMap.OnMyLocationButtonClickListener {

    companion object{
        lateinit var mainContext: Context
        var activatedGPS: Boolean = true

        lateinit var totalsSelectedSport: Totals
       /* lateinit var totalsBike: Totals
        lateinit var totalsRollerSkate: Totals*/

        lateinit var totalsRunning: Totals

        val REQUIRED_PERMISSIONS_GPS =
            arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION)
    }


    private var mHandler: Handler? = null
    private var mInterval = 1000
    private var timeInSeconds = 0L
    private var rounds: Int = 1
    private var startButtonClicked = false


    private var widthScreenPixels: Int = 0
    private var heightScreenPixels: Int = 0
    private var widthAnimations: Int = 0

    private lateinit var drawer: DrawerLayout

    private lateinit var csbChallengeDistance: CircularSeekBar
    private lateinit var csbCurrentDistance: CircularSeekBar
    private lateinit var csbRecordDistance: CircularSeekBar

    private lateinit var csbCurrentAvgSpeed: CircularSeekBar
    private lateinit var csbRecordAvgSpeed: CircularSeekBar

    private lateinit var csbCurrentSpeed: CircularSeekBar
    private lateinit var csbCurrentMaxSpeed: CircularSeekBar
    private lateinit var csbRecordSpeed: CircularSeekBar

    private lateinit var tvDistanceRecord: TextView
    private lateinit var tvAvgSpeedRecord: TextView
    private lateinit var tvMaxSpeedRecord: TextView

    private lateinit var tvChrono: TextView
    private lateinit var fbCamara: FloatingActionButton

    private lateinit var swIntervalMode: Switch
    private lateinit var npDurationInterval: NumberPicker
    private lateinit var tvRunningTime: TextView
    private lateinit var tvWalkingTime: TextView
    private lateinit var csbRunWalk: CircularSeekBar

    private lateinit var swChallenges: Switch

    private lateinit var npChallengeDistance: NumberPicker
    private lateinit var npChallengeDurationHH: NumberPicker
    private lateinit var npChallengeDurationMM: NumberPicker
    private lateinit var npChallengeDurationSS: NumberPicker
    private var challengeDistance: Float = 0f
    private var challengeDuration: Int = 0


    private lateinit var swVolumes: Switch
    private var mpNotify : MediaPlayer? = null
    private var mpHard : MediaPlayer? = null
    private var mpSoft : MediaPlayer? = null
    private lateinit var sbHardVolume : SeekBar
    private lateinit var sbSoftVolume : SeekBar
    private lateinit var sbNotifyVolume : SeekBar

    private lateinit var sbHardTrack : SeekBar
    private lateinit var sbSoftTrack : SeekBar

    private var ROUND_INTERVAL = 300
    private var hardTime: Boolean = true
    private var TIME_RUNNING: Int = 0

    private var LIMIT_DISTANCE_ACCEPTED: Double = 0.0
    private var sportSelected: String = "Running"

    private lateinit var lyPopupRun: LinearLayout

    private lateinit var map: GoogleMap
    private var mapCentered = true
    private lateinit var listPoints: Iterable<LatLng>

    private var controlRunningWalking: Boolean = true

    private val PERMISSION_ID = 42
    private val LOCATION_PERMISSION_REQ_CODE = 1000;


    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var flagSavedLocation = false

    private var latitude: Double = 0.0
    private var longitude: Double = 0.0
    private var init_lt: Double = 0.0
    private var init_ln: Double = 0.0

    private var distance: Double = 0.0
    private var maxSpeed: Double = 0.0
    private var avgSpeed: Double = 0.0
    private var speed: Double = 0.0

    private var minAltitude: Double? = null
    private var maxAltitude: Double? = null
    private var minLatitude: Double? = null
    private var maxLatitude: Double? = null
    private var minLongitude: Double? = null
    private var maxLongitude: Double? = null


    private lateinit var levelRunning: Level
    private lateinit var levelSelectedSport: Level


    private lateinit var levelsListRunning: ArrayList<Level>

    private var sportsLoaded: Int = 0

    private lateinit var dateRun: String
    private lateinit var startTimeRun: String


    private lateinit var medalsListRunningDistance: ArrayList<Double>
    private lateinit var medalsListRunningAvgSpeed: ArrayList<Double>
    private lateinit var medalsListRunningMaxSpeed: ArrayList<Double>

    private lateinit var medalsListSportSelectedDistance: ArrayList<Double>
    private lateinit var medalsListSportSelectedAvgSpeed: ArrayList<Double>
    private lateinit var medalsListSportSelectedMaxSpeed: ArrayList<Double>


    private var recDistanceGold: Boolean = false
    private var recDistanceSilver: Boolean = false
    private var recDistanceBronze: Boolean = false
    private var recAvgSpeedGold: Boolean = false
    private var recAvgSpeedSilver: Boolean = false
    private var recAvgSpeedBronze: Boolean = false
    private var recMaxSpeedGold: Boolean = false
    private var recMaxSpeedSilver: Boolean = false
    private var recMaxSpeedBronze: Boolean = false

    ////////////////

    ///////////////
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mainContext = this

        initObjects()


        initToolBar()
        initNavigationView()
        initPermissionsGPS()

        loadFromDB()

        Toast.makeText(this, "Bienvenido $userEmail",
            Toast.LENGTH_SHORT).show()
    }

    override fun onBackPressed() {
        super.onBackPressed()

        if (drawer.isDrawerOpen(GravityCompat.START))
            drawer.closeDrawer(GravityCompat.START)
        else
            signOut()
    }

    private fun initToolBar(){
        val toolbar: androidx.appcompat.widget.Toolbar = findViewById(R.id.toolbar_main)
        setSupportActionBar(toolbar)

        drawer = findViewById(R.id.drawer_layout)
        val toggle = ActionBarDrawerToggle(
            this, drawer, toolbar, R.string.bar_title,
            R.string.navigation_drawer_close)

        drawer.addDrawerListener(toggle)

        toggle.syncState()
    }

    private fun initNavigationView(){
        var navigationView: NavigationView = findViewById(R.id.nav_view)
        navigationView.setNavigationItemSelectedListener(this)

        var headerView: View = LayoutInflater.from(this).inflate(R.layout.nav_header_main, navigationView, false)
        navigationView.removeHeaderView(headerView)
        navigationView.addHeaderView(headerView)

        var tvUser: TextView = headerView.findViewById(R.id.tvUser)
        tvUser.text = userEmail
    }

    private fun initStopWatch() {
        tvChrono.text = getString(R.string.init_stop_watch_value)
    }

    private fun initChrono(){

        tvChrono = findViewById(R.id.tvChrono)
        tvChrono.setTextColor(ContextCompat.getColor( this, R.color.white))
        initStopWatch()

        widthScreenPixels = resources.displayMetrics.widthPixels
        heightScreenPixels = resources.displayMetrics.heightPixels

        widthAnimations = widthScreenPixels


        val lyChronoProgressBg = findViewById<LinearLayout>(R.id.lyChronoProgressBg)
        val lyRoundProgressBg = findViewById<LinearLayout>(R.id.lyRoundProgressBg)
        lyChronoProgressBg.translationX = -widthAnimations.toFloat()
        lyRoundProgressBg.translationX = -widthAnimations.toFloat()

        val tvReset: TextView = findViewById(R.id.tvReset)
        tvReset.setOnClickListener { resetClicked()  }

        fbCamara = findViewById(R.id.fbCamera)
        fbCamara.isVisible = false
    }

    private fun hideLayouts(){
        var lyMap = findViewById<LinearLayout>(R.id.lyMap)
        var lyFragmentMap = findViewById<LinearLayout>(R.id.lyFragmentMap)
        val lyIntervalModeSpace = findViewById<LinearLayout>(R.id.lyIntervalModeSpace)
        val lyIntervalMode = findViewById<LinearLayout>(R.id.lyIntervalMode)
        val lyChallengesSpace = findViewById<LinearLayout>(R.id.lyChallengesSpace)
        val lyChallenges = findViewById<LinearLayout>(R.id.lyChallenges)
        val lySettingsVolumesSpace = findViewById<LinearLayout>(R.id.lySettingsVolumesSpace)
        val lySettingsVolumes = findViewById<LinearLayout>(R.id.lySettingsVolumes)
        var lySoftTrack = findViewById<LinearLayout>(R.id.lySoftTrack)
        var lySoftVolume = findViewById<LinearLayout>(R.id.lySoftVolume)

        setHeightLinearLayout(lyMap, 0)
        setHeightLinearLayout(lyIntervalModeSpace,0)
        setHeightLinearLayout(lyChallengesSpace,0)
        setHeightLinearLayout(lySettingsVolumesSpace,0)
        setHeightLinearLayout(lySoftTrack,0)
        setHeightLinearLayout(lySoftVolume,0)

        lyFragmentMap.translationY = -300f
        lyIntervalMode.translationY = -300f
        lyChallenges.translationY = -300f
        lySettingsVolumes.translationY = -300f
    }

    private fun initMetrics(){
        csbCurrentDistance = findViewById(R.id.csbCurrentDistance)
        csbChallengeDistance = findViewById(R.id.csbChallengeDistance)
        csbRecordDistance = findViewById(R.id.csbRecordDistance)

        csbCurrentAvgSpeed = findViewById(R.id.csbCurrentAvgSpeed)
        csbRecordAvgSpeed = findViewById(R.id.csbRecordAvgSpeed)

        csbCurrentSpeed = findViewById(R.id.csbCurrentSpeed)
        csbCurrentMaxSpeed = findViewById(R.id.csbCurrentMaxSpeed)
        csbRecordSpeed = findViewById(R.id.csbRecordSpeed)

        csbCurrentDistance.progress = 0f
        csbChallengeDistance.progress = 0f

        csbCurrentAvgSpeed.progress = 0f

        csbCurrentSpeed.progress = 0f
        csbCurrentMaxSpeed.progress = 0f

        tvDistanceRecord = findViewById(R.id.tvDistanceRecord)
        tvAvgSpeedRecord = findViewById(R.id.tvAvgSpeedRecord)
        tvMaxSpeedRecord = findViewById(R.id.tvMaxSpeedRecord)

        tvDistanceRecord.text = ""
        tvAvgSpeedRecord.text = ""
        tvMaxSpeedRecord.text = ""
    }

    private fun initSwitchs(){
        swIntervalMode = findViewById(R.id.swIntervalMode)
        swChallenges = findViewById(R.id.swChallenges)
        swVolumes = findViewById(R.id.swVolumes)
    }

    private fun initIntervalMode(){
        npDurationInterval = findViewById(R.id.npDurationInterval)
        tvRunningTime = findViewById(R.id.tvRunningTime)
        tvWalkingTime = findViewById(R.id.tvWalkingTime)
        csbRunWalk = findViewById(R.id.csbRunWalk)

        npDurationInterval.minValue = 1
        npDurationInterval.maxValue = 60
        npDurationInterval.value = 5
        npDurationInterval.wrapSelectorWheel = true
        npDurationInterval.setFormatter(NumberPicker.Formatter { i -> String.format("%02d", i) })

        npDurationInterval.setOnValueChangedListener { picker, oldVal, newVal ->
            csbRunWalk.max = (newVal*60).toFloat()
            csbRunWalk.progress = csbRunWalk.max/2


            tvRunningTime.text = getFormattedStopWatch(((newVal*60/2)*1000).toLong()).subSequence(3,8)
            tvWalkingTime.text = tvRunningTime.text

            ROUND_INTERVAL = newVal * 60
            TIME_RUNNING = ROUND_INTERVAL / 2
        }

        csbRunWalk.max = 300f
        csbRunWalk.progress = 150f
        csbRunWalk.setOnSeekBarChangeListener(object :
            CircularSeekBar.OnCircularSeekBarChangeListener {
            override fun onProgressChanged(
                circularSeekBar: CircularSeekBar?,
                progress: Float,
                fromUser: Boolean
            ) {

                if (fromUser){
                    var STEPS_UX: Int = 15
                    if (ROUND_INTERVAL > 600) STEPS_UX = 60
                    if (ROUND_INTERVAL > 1800) STEPS_UX = 300
                    var set: Int = 0
                    var p = progress.toInt()

                    var limit = 60
                    if (ROUND_INTERVAL > 1800) limit = 300

                    if (p%STEPS_UX != 0 && progress != csbRunWalk.max){
                        while (p >= limit) p -= limit
                        while (p >= STEPS_UX) p -= STEPS_UX
                        if (STEPS_UX-p > STEPS_UX/2) set = -1 * p
                        else set = STEPS_UX-p

                        if (csbRunWalk.progress + set > csbRunWalk.max)
                            csbRunWalk.progress = csbRunWalk.max
                        else
                            csbRunWalk.progress += set
                    }
                }
                if (csbRunWalk.progress == 0f) manageEnableButtonsRun(false, false)
                else manageEnableButtonsRun(false, true)

                tvRunningTime.text = getFormattedStopWatch((csbRunWalk.progress.toInt() *1000).toLong()).subSequence(3,8)
                tvWalkingTime.text = getFormattedStopWatch(((ROUND_INTERVAL- csbRunWalk.progress.toInt())*1000).toLong()).subSequence(3,8)
                TIME_RUNNING = getSecFromWatch(tvRunningTime.text.toString())
            }

            override fun onStopTrackingTouch(seekBar: CircularSeekBar?) {
            }

            override fun onStartTrackingTouch(seekBar: CircularSeekBar?) {
            }
        })
    }

    private fun initChallengeMode(){
        npChallengeDistance = findViewById(R.id.npChallengeDistance)
        npChallengeDurationHH = findViewById(R.id.npChallengeDurationHH)
        npChallengeDurationMM = findViewById(R.id.npChallengeDurationMM)
        npChallengeDurationSS = findViewById(R.id.npChallengeDurationSS)

        npChallengeDistance.minValue = 1
        npChallengeDistance.maxValue = 300
        npChallengeDistance.value = 10
        npChallengeDistance.wrapSelectorWheel = true


        npChallengeDistance.setOnValueChangedListener { picker, oldVal, newVal ->
            challengeDistance = newVal.toFloat()
            csbChallengeDistance.max = newVal.toFloat()
            csbChallengeDistance.progress = newVal.toFloat()
            challengeDuration = 0

            if (csbChallengeDistance.max > csbRecordDistance.max)
                csbCurrentDistance.max = csbChallengeDistance.max
        }

        npChallengeDurationHH.minValue = 0
        npChallengeDurationHH.maxValue = 23
        npChallengeDurationHH.value = 1
        npChallengeDurationHH.wrapSelectorWheel = true
        npChallengeDurationHH.setFormatter({ i -> String.format("%02d", i) })

        npChallengeDurationMM.minValue = 0
        npChallengeDurationMM.maxValue = 59
        npChallengeDurationMM.value = 0
        npChallengeDurationMM.wrapSelectorWheel = true
        npChallengeDurationMM.setFormatter(NumberPicker.Formatter { i -> String.format("%02d", i) })

        npChallengeDurationSS.minValue = 0
        npChallengeDurationSS.maxValue = 59
        npChallengeDurationSS.value = 0
        npChallengeDurationSS.wrapSelectorWheel = true
        npChallengeDurationSS.setFormatter(NumberPicker.Formatter { i -> String.format("%02d", i) })

        npChallengeDurationHH.setOnValueChangedListener { picker, oldVal, newVal ->
            getChallengeDuration(newVal, npChallengeDurationMM.value, npChallengeDurationSS.value)
        }
        npChallengeDurationMM.setOnValueChangedListener { picker, oldVal, newVal ->
            getChallengeDuration(npChallengeDurationHH.value, newVal, npChallengeDurationSS.value)
        }
        npChallengeDurationSS.setOnValueChangedListener { picker, oldVal, newVal ->
            getChallengeDuration(npChallengeDurationHH.value, npChallengeDurationMM.value, newVal)
        }

    }

    private fun setVolumes(){
        sbHardVolume.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(p0: SeekBar?, i: Int, p2: Boolean) {
                mpHard?.setVolume(i/100.0f, i/100.0f)
            }
            override fun onStartTrackingTouch(p0: SeekBar?) { }
            override fun onStopTrackingTouch(p0: SeekBar?) { }
        })

        sbSoftVolume.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(p0: SeekBar?, i: Int, p2: Boolean) {
                mpSoft?.setVolume(i/100.0f, i/100.0f)
            }
            override fun onStartTrackingTouch(p0: SeekBar?) { }
            override fun onStopTrackingTouch(p0: SeekBar?) { }
        })
        sbNotifyVolume.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(p0: SeekBar?, i: Int, p2: Boolean) {
                mpNotify?.setVolume(i/100.0f, i/100.0f)
            }
            override fun onStartTrackingTouch(p0: SeekBar?) { }
            override fun onStopTrackingTouch(p0: SeekBar?) { }
        })
    }

    private fun updateTimesTrack(timesH: Boolean, timesS: Boolean){
        val sbHardTrack = findViewById<SeekBar>(R.id.sbHardTrack)
        val sbSoftTrack = findViewById<SeekBar>(R.id.sbSoftTrack)

        if (timesH){
            val tvHardPosition = findViewById<TextView>(R.id.tvHardPosition)
            val tvHardRemaining = findViewById<TextView>(R.id.tvHardRemaining)
            tvHardPosition.text = getFormattedStopWatch(sbHardTrack.progress.toLong())
            tvHardRemaining.text = "-" + getFormattedStopWatch( mpHard!!.duration.toLong() - sbHardTrack.progress.toLong())
        }
        if (timesS){
            val tvSoftPosition = findViewById<TextView>(R.id.tvSoftPosition)
            val tvSoftRemaining = findViewById<TextView>(R.id.tvSoftRemaining)
            tvSoftPosition.text = getFormattedStopWatch(sbSoftTrack.progress.toLong())
            tvSoftRemaining.text = "-" + getFormattedStopWatch( mpSoft!!.duration.toLong() - sbSoftTrack.progress.toLong())
        }
    }

    private fun setProgressTracks(){
        val sbHardTrack = findViewById<SeekBar>(R.id.sbHardTrack)
        val sbSoftTrack = findViewById<SeekBar>(R.id.sbSoftTrack)
        sbHardTrack.max = mpHard!!.duration
        sbSoftTrack.max = mpSoft!!.duration
        updateTimesTrack(true, true)

        sbHardTrack.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(p0: SeekBar?, i: Int, fromUser: Boolean) {
                if (fromUser){
                    mpHard?.pause()
                    mpHard?.seekTo(i)
                    mpHard?.start()
                    updateTimesTrack(true, false)
                }
            }
            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(p0: SeekBar?) {}
        })

        sbSoftTrack.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(p0: SeekBar?, i: Int, fromUser: Boolean) {
                if (fromUser){
                    mpSoft?.pause()
                    mpSoft?.seekTo(i)
                    mpSoft?.start()
                    updateTimesTrack(false, true)
                }
            }
            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(p0: SeekBar?) {}
        })

    }

    private fun initMusic(){
        mpNotify = MediaPlayer.create(this, R.raw.notification)
        mpHard = MediaPlayer.create(this, R.raw.runningmusic)
        mpSoft = MediaPlayer.create(this, R.raw.walkingmusic)

        mpHard?.isLooping = true
        mpSoft?.isLooping = true
        mpNotify?.isLooping = false


        sbHardVolume = findViewById(R.id.sbHardVolume)
        sbSoftVolume = findViewById(R.id.sbSoftVolume)
        sbNotifyVolume = findViewById(R.id.sbNotifyVolume)

        sbHardTrack = findViewById(R.id.sbHardTrack)
        sbSoftTrack = findViewById(R.id.sbSoftTrack)

        setVolumes()
        setProgressTracks()
    }

    private fun notifySound(){
        mpNotify?.start()
    }


    private fun initObjects(){

        initChrono()
        hideLayouts()
        initMetrics()
        initSwitchs()
        initIntervalMode()
        initChallengeMode()
        initMusic()
        hidePopUpRun()

        initMap()

        initTotals()
        initLevels()
        initMedals()



    }

    private fun initTotals(){
        /* totalsBike = Totals()
        totalsRollerSkate = Totals() */
        totalsRunning = Totals()

       /* totalsBike.totalRuns = 0
        totalsBike.totalDistance = 0.0
        totalsBike.totalTime = 0
        totalsBike.recordDistance = 0.0
        totalsBike.recordSpeed = 0.0
        totalsBike.recordAvgSpeed = 0.0*/

     /*   totalsRollerSkate.totalRuns = 0
        totalsRollerSkate.totalDistance = 0.0
        totalsRollerSkate.totalTime = 0
        totalsRollerSkate.recordDistance = 0.0
        totalsRollerSkate.recordSpeed = 0.0
        totalsRollerSkate.recordAvgSpeed = 0.0 */

        totalsRunning.totalRuns = 0
        totalsRunning.totalDistance = 0.0
        totalsRunning.totalTime = 0
        totalsRunning.recordDistance = 0.0
        totalsRunning.recordSpeed = 0.0
        totalsRunning.recordAvgSpeed = 0.0

    }
    private fun initLevels(){
        levelSelectedSport = Level()
       /* levelBike = Level()
        levelRollerSkate = Level()*/
        levelRunning = Level()

       /* levelsListBike = arrayListOf()
        levelsListBike.clear()

        levelsListRollerSkate = arrayListOf()
        levelsListRollerSkate.clear()*/

        levelsListRunning = arrayListOf()
        levelsListRunning.clear()

      /*  levelBike.name = "turtle"
        levelBike.image = "level_1"
        levelBike.RunsTarget = 5
        levelBike.DistanceTarget = 40

        levelRollerSkate.name = "turtle"
        levelRollerSkate.image = "level_1"
        levelRollerSkate.RunsTarget = 5
        levelRollerSkate.DistanceTarget = 20 */

        levelRunning.name = "Trotador Novato"
        levelRunning.image = "level_1"
        levelRunning.RunsTarget = 5
        levelRunning.DistanceTarget = 10
    }
    private fun initMedals(){
        medalsListSportSelectedDistance = arrayListOf()
        medalsListSportSelectedAvgSpeed = arrayListOf()
        medalsListSportSelectedMaxSpeed = arrayListOf()
        medalsListSportSelectedDistance.clear()
        medalsListSportSelectedAvgSpeed.clear()
        medalsListSportSelectedMaxSpeed.clear()

       /* medalsListBikeDistance = arrayListOf()
        medalsListBikeAvgSpeed = arrayListOf()
        medalsListBikeMaxSpeed = arrayListOf()
        medalsListBikeDistance.clear()
        medalsListBikeAvgSpeed.clear()
        medalsListBikeMaxSpeed.clear()

        medalsListRollerSkateDistance = arrayListOf()
        medalsListRollerSkateAvgSpeed = arrayListOf()
        medalsListRollerSkateMaxSpeed = arrayListOf()
        medalsListRollerSkateDistance.clear()
        medalsListRollerSkateAvgSpeed.clear()
        medalsListRollerSkateMaxSpeed.clear()*/

        medalsListRunningDistance = arrayListOf()
        medalsListRunningAvgSpeed = arrayListOf()
        medalsListRunningMaxSpeed = arrayListOf()
        medalsListRunningDistance.clear()
        medalsListRunningAvgSpeed.clear()
        medalsListRunningMaxSpeed.clear()
    }

    private fun loadFromDB(){
        loadTotalsUser()
        loadMedalsUser()
    }
    private fun loadTotalsUser(){
       /* loadTotalSport("Bike")
        loadTotalSport("RollerSkate")*/
        loadTotalSport("Running")

    }
    private fun loadTotalSport(sport: String){
        var collection = "totals$sport"
        var dbTotalsUser = FirebaseFirestore.getInstance()
        dbTotalsUser.collection(collection).document(userEmail)
            .get()
            .addOnSuccessListener { document ->
                if (document.data?.size != null){
                    var total = document.toObject(Totals::class.java)
                    when (sport){
                        /*"Bike" -> totalsBike = total!!
                        "RollerSkate" -> totalsRollerSkate = total!!*/
                        "Running" -> totalsRunning = total!!
                    }

                }
                else{
                    val dbTotal: FirebaseFirestore = FirebaseFirestore.getInstance()
                    dbTotal.collection(collection).document(userEmail).set(hashMapOf(
                        "recordAvgSpeed" to 0.0,
                        "recordDistance" to 0.0,
                        "recordSpeed" to 0.0,
                        "totalDistance" to 0.0,
                        "totalRuns" to 0,
                        "totalTime" to 0
                    ))
                }
                sportsLoaded++
                setLevelSport(sport)
                if (sportsLoaded >= 1) selectSport(sportSelected)

            }
            .addOnFailureListener { exception ->
                Log.d("ERROR loadTotalsUser", "get failed with ", exception)
            }

    }

    private fun setLevelSport(sport: String){
        val dbLevels: FirebaseFirestore = FirebaseFirestore.getInstance()
        dbLevels.collection("levels$sport")
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents){
                    when (sport){
                        /*"Bike" -> levelsListBike.add(document.toObject(Level::class.java))
                        "RollerSkate" -> levelsListRollerSkate.add(document.toObject(Level::class.java))*/
                        "Running" -> levelsListRunning.add(document.toObject(Level::class.java))
                    }

                }
                when (sport){
                  /*  "Bike" -> setLevelBike()
                    "RollerSkate" -> setLevelRollerSkate()*/
                    "Running" -> setLevelRunning()
                }

            }
            .addOnFailureListener { exception ->
                Log.w(ContentValues.TAG, "Error getting documents: ", exception)
            }
    }

    private fun setLevelRunning(){
        var lyNavLevelRunning = findViewById<LinearLayout>(R.id.lyNavLevelRunning)
        if (totalsRunning.totalTime!! == 0) setHeightLinearLayout(lyNavLevelRunning, 0)
        else{

            setHeightLinearLayout(lyNavLevelRunning, 300)
            for (level in levelsListRunning){
                if (totalsRunning.totalRuns!! < level.RunsTarget!!.toInt()
                    || totalsRunning.totalDistance!! < level.DistanceTarget!!.toDouble()){

                    levelRunning.name = level.name!!
                    levelRunning.image = level.image!!
                    levelRunning.RunsTarget = level.RunsTarget!!
                    levelRunning.DistanceTarget = level.DistanceTarget!!

                    break
                }
            }

            var ivLevelRunning = findViewById<ImageView>(R.id.ivLevelRunning)
            var tvTotalTimeRunning = findViewById<TextView>(R.id.tvTotalTimeRunning)
            var tvTotalRunsRunning = findViewById<TextView>(R.id.tvTotalRunsRunning)
            var tvTotalDistanceRunning = findViewById<TextView>(R.id.tvTotalDistanceRunning)


            var tvNumberLevelRunning = findViewById<TextView>(R.id.tvNumberLevelRunning)
            var levelText = "${getString(R.string.level)} ${levelRunning.image!!.subSequence(6,7).toString()}"
            tvNumberLevelRunning.text = levelText

            var tt = getFormattedTotalTime(totalsRunning.totalTime!!.toLong())
            tvTotalTimeRunning.text = tt

            when (levelRunning.image){
                "level_1" -> ivLevelRunning.setImageResource(R.drawable.level_1)
                "level_2" -> ivLevelRunning.setImageResource(R.drawable.level_2)
                "level_3" -> ivLevelRunning.setImageResource(R.drawable.level_2)
                "level_4" -> ivLevelRunning.setImageResource(R.drawable.level_2)
                "level_5" -> ivLevelRunning.setImageResource(R.drawable.level_2)
                "level_6" -> ivLevelRunning.setImageResource(R.drawable.level_2)
                "level_7" -> ivLevelRunning.setImageResource(R.drawable.level_2)
            }

            tvTotalRunsRunning.text = "${totalsRunning.totalRuns}/${levelRunning.RunsTarget}"
            var porcent = totalsRunning.totalDistance!!.toInt() * 100 / levelRunning.DistanceTarget!!.toInt()
            tvTotalDistanceRunning.text = "${porcent.toInt()}%"

            var csbDistanceRunning = findViewById<CircularSeekBar>(R.id.csbDistanceRunning)
            csbDistanceRunning.max = levelRunning.DistanceTarget!!.toFloat()
            if (totalsRunning.totalDistance!! >= levelRunning.DistanceTarget!!.toDouble())
                csbDistanceRunning.progress = csbDistanceRunning.max
            else
                csbDistanceRunning.progress = totalsRunning.totalDistance!!.toFloat()

            var csbRunsRunning = findViewById<CircularSeekBar>(R.id.csbRunsRunning)
            csbRunsRunning.max = levelRunning.RunsTarget!!.toFloat()
            if (totalsRunning.totalRuns!! >= levelRunning.RunsTarget!!.toInt())
                csbRunsRunning.progress = csbRunsRunning.max
            else
                csbRunsRunning.progress = totalsRunning.totalRuns!!.toFloat()

        }
    }

    private fun loadMedalsUser(){
        loadMedalsRunning()
    }


    private fun loadMedalsRunning(){
        var dbRecords = FirebaseFirestore.getInstance()
        dbRecords.collection("runsRunning")
            .orderBy("distance", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents){
                    if (document["user"] == userEmail)
                        medalsListRunningDistance.add (document["distance"].toString().toDouble())
                    if (medalsListRunningDistance.size == 3) break
                }
                while (medalsListRunningDistance.size < 3) medalsListRunningDistance.add(0.0)

            }
            .addOnFailureListener { exception ->
                Log.w(ContentValues.TAG, "Error getting documents: ", exception)
            }

        dbRecords.collection("runsRunning")
            .orderBy("avgSpeed", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    if (document["user"] == userEmail)
                        medalsListRunningAvgSpeed.add (document["avgSpeed"].toString().toDouble())
                    if (medalsListRunningAvgSpeed.size == 3) break
                }
                while (medalsListRunningAvgSpeed.size < 3) medalsListRunningAvgSpeed.add(0.0)

            }
            .addOnFailureListener { exception ->
                Log.w(ContentValues.TAG, "Error getting documents: ", exception)
            }

        dbRecords.collection("runsRunning")
            .orderBy("maxSpeed", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    if (document["user"] == userEmail)
                        medalsListRunningMaxSpeed.add (document["maxSpeed"].toString().toDouble())
                    if (medalsListRunningMaxSpeed.size == 3) break
                }
                while (medalsListRunningMaxSpeed.size < 3) medalsListRunningMaxSpeed.add(0.0)

            }
            .addOnFailureListener { exception ->
                Log.w(ContentValues.TAG, "Error getting documents: ", exception)
            }
    }




    override fun onNavigationItemSelected(item: MenuItem): Boolean {

        when (item.itemId){
            R.id.nav_item_record -> callRecordActivity()
            R.id.nav_item_signout -> signOut()
        }

        drawer.closeDrawer(GravityCompat.START)

        return true
    }

    fun callSignOut(view: View){
        signOut()
    }

    private fun signOut(){
        userEmail= ""

        if (providerSession == "Facebook")  LoginManager.getInstance().logOut()

        FirebaseAuth.getInstance().signOut()
        startActivity(Intent(this, LoginActivity::class.java))
    }

    private fun callRecordActivity(){
        val intent = Intent(this, RecordActivity::class.java)
        startActivity(intent)
    }

    fun inflateIntervalMode(v: View){
        val lyIntervalMode = findViewById<LinearLayout>(R.id.lyIntervalMode)
        val lyIntervalModeSpace = findViewById<LinearLayout>(R.id.lyIntervalModeSpace)
        var lySoftTrack = findViewById<LinearLayout>(R.id.lySoftTrack)
        var lySoftVolume = findViewById<LinearLayout>(R.id.lySoftVolume)
        var tvRounds = findViewById<TextView>(R.id.tvRounds)

        if (swIntervalMode.isChecked){
            animateViewofInt(swIntervalMode, "textColor", ContextCompat.getColor(this, R.color.orange), 500)
            setHeightLinearLayout(lyIntervalModeSpace, 600)
            animateViewofFloat(lyIntervalMode, "translationY", 0f, 500)
            animateViewofFloat (tvChrono, "translationX", -110f, 500)
            tvRounds.setText(R.string.rounds)
            animateViewofInt(tvRounds, "textColor", ContextCompat.getColor(this, R.color.white), 500)

            setHeightLinearLayout(lySoftTrack,120)
            setHeightLinearLayout(lySoftVolume,200)
            if (swVolumes.isChecked){
                var lySettingsVolumesSpace = findViewById<LinearLayout>(R.id.lySettingsVolumesSpace)
                setHeightLinearLayout(lySettingsVolumesSpace,600)
            }
        }
        else{
            swIntervalMode.setTextColor(ContextCompat.getColor(this, R.color.white))
            setHeightLinearLayout(lyIntervalModeSpace,0)
            lyIntervalMode.translationY = -200f
            animateViewofFloat (tvChrono, "translationX", 0f, 500)
            tvRounds.text = ""
            setHeightLinearLayout(lySoftTrack,0)
            setHeightLinearLayout(lySoftVolume,0)
            if (swVolumes.isChecked){
                var lySettingsVolumesSpace = findViewById<LinearLayout>(R.id.lySettingsVolumesSpace)
                setHeightLinearLayout(lySettingsVolumesSpace,400)
            }
        }
    }

    /*fun callInflateIntervalMode(v: View) {
        inflateIntervalMode()
    }*/
    

    fun inflateChallenges(v: View){
        val lyChallengesSpace = findViewById<LinearLayout>(R.id.lyChallengesSpace)
        val lyChallenges = findViewById<LinearLayout>(R.id.lyChallenges)
        if (swChallenges.isChecked){
            animateViewofInt(swChallenges, "textColor", ContextCompat.getColor(this, R.color.orange), 500)
            setHeightLinearLayout(lyChallengesSpace, 750)
            animateViewofFloat(lyChallenges, "translationY", 0f, 500)
        }
        else{
            swChallenges.setTextColor(ContextCompat.getColor(this, R.color.white))
            setHeightLinearLayout(lyChallengesSpace,0)
            lyChallenges.translationY = -300f

            challengeDistance = 0f
            challengeDuration = 0
        }
    }

    fun showDuration(v: View){
        if (timeInSeconds.toInt() == 0) showChallenge("duration")
    }

    fun showDistance(v:View){
        if (timeInSeconds.toInt() == 0) showChallenge("distance")
    }

    private fun showChallenge(option: String){
        var lyChallengeDuration = findViewById<LinearLayout>(R.id.lyChallengeDuration)
        var lyChallengeDistance = findViewById<LinearLayout>(R.id.lyChallengeDistance)
        var tvChallengeDuration = findViewById<TextView>(R.id.tvChallengeDuration)
        var tvChallengeDistance = findViewById<TextView>(R.id.tvChallengeDistance)

        when (option){
            "duration" ->{
                lyChallengeDuration.translationZ = 5f
                lyChallengeDistance.translationZ = 0f

                tvChallengeDuration.setTextColor(ContextCompat.getColor(this, R.color.orange))
                tvChallengeDuration.setBackgroundColor(ContextCompat.getColor(this, R.color.gray_dark))

                tvChallengeDistance.setTextColor(ContextCompat.getColor(this, R.color.white))
                tvChallengeDistance.setBackgroundColor(ContextCompat.getColor(this, R.color.gray_medium))

                challengeDistance = 0f
                getChallengeDuration(npChallengeDurationHH.value, npChallengeDurationMM.value, npChallengeDurationSS.value)
            }
            "distance" -> {
                lyChallengeDuration.translationZ = 0f
                lyChallengeDistance.translationZ = 5f

                tvChallengeDuration.setTextColor(ContextCompat.getColor(this, R.color.white))
                tvChallengeDuration.setBackgroundColor(ContextCompat.getColor(this, R.color.gray_medium))

                tvChallengeDistance.setTextColor(ContextCompat.getColor(this, R.color.orange))
                tvChallengeDistance.setBackgroundColor(ContextCompat.getColor(this, R.color.gray_dark))

                challengeDuration = 0
                challengeDistance = npChallengeDistance.value.toFloat()
            }
        }
    }

    private fun getChallengeDuration(hh: Int, mm: Int, ss: Int){
        var hours: String = hh.toString()
        if (hh<10) hours = "0"+hours
        var minutes: String = mm.toString()
        if (mm<10) minutes = "0"+minutes
        var seconds: String = ss.toString()
        if (ss<10) seconds = "0"+seconds

        challengeDuration = getSecFromWatch("${hours}:${minutes}:${seconds}")
    }

    fun inflateVolumes(v: View){

        val lySettingsVolumesSpace = findViewById<LinearLayout>(R.id.lySettingsVolumesSpace)
        val lySettingsVolumes = findViewById<LinearLayout>(R.id.lySettingsVolumes)

        if (swVolumes.isChecked){
            animateViewofInt(swVolumes, "textColor", ContextCompat.getColor(this, R.color.orange), 500)
            var swIntervalMode = findViewById<Switch>(R.id.swIntervalMode)
            var value = 400
            if (swIntervalMode.isChecked) value = 600

            setHeightLinearLayout(lySettingsVolumesSpace, value)
            animateViewofFloat(lySettingsVolumes, "translationY", 0f, 500)
        }
        else{
            swVolumes.setTextColor(ContextCompat.getColor(this, R.color.white))
            setHeightLinearLayout(lySettingsVolumesSpace,0)
            lySettingsVolumes.translationY = -300f
        }
    }

    private fun initMap(){

        listPoints = arrayListOf()
        (listPoints as ArrayList<LatLng>).clear()

        createMapFragment()

        createMapFragment()
        var lyOpenerButton = findViewById<LinearLayout>(R.id.lyOpenerButton)
        if (allPermissionsGrantedGPS()) lyOpenerButton.isEnabled = true
        else lyOpenerButton.isEnabled = false
    }

    override fun onMyLocationButtonClick(): Boolean {
        return false
    }

    override fun onMyLocationClick(p0: Location){

    }

    private fun createMapFragment(){
        val mapFragment = supportFragmentManager.findFragmentById(R.id.fragmentMap) as SupportMapFragment?
        mapFragment?.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        googleMap.mapType = GoogleMap.MAP_TYPE_HYBRID
        enableMyLocation()
        map.setOnMyLocationButtonClickListener (this)
        map.setOnMyLocationClickListener(this)
        map.setOnMapLongClickListener {  mapCentered = false }
        map.setOnMapClickListener { mapCentered = false  }

        manageLocation()
        centerMap (init_lt ,init_ln)

    }

    override fun onRequestPermissionsResult(requestCode: Int,permissions: Array<out String>,grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode){
            LOCATION_PERMISSION_REQ_CODE -> {
                var lyOpenerButton = findViewById<LinearLayout>(R.id.lyOpenerButton)

                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    lyOpenerButton.isEnabled = true
                else{
                    var lyMap = findViewById<LinearLayout>(R.id.lyMap)
                    if (lyMap.height > 0){
                        setHeightLinearLayout(lyMap, 0)

                        var lyFragmentMap = findViewById<LinearLayout>(R.id.lyFragmentMap)
                        lyFragmentMap.translationY= -300f

                        var ivOpenClose = findViewById<ImageView>(R.id.ivOpenClose)
                        ivOpenClose.setRotation(0f)
                    }

                    lyOpenerButton.isEnabled = false

                }
            }
        }
    }

    private fun enableMyLocation(){
        if (!::map.isInitialized) return
        if (ActivityCompat.checkSelfPermission(  this,Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED

            && ActivityCompat.checkSelfPermission(this,Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLocation()
            return
        }
        else map.isMyLocationEnabled = true
    }

    private fun centerMap(lt: Double, ln: Double){
        val posMap = LatLng(lt, ln)
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(posMap, 16f), 1000, null)

    }

    fun changeTypeMap(v: View){
        var ivTypeMap = findViewById<ImageView>(R.id.ivTypeMap)
        if (map.mapType == GoogleMap.MAP_TYPE_HYBRID){
            map.mapType = GoogleMap.MAP_TYPE_NORMAL
            ivTypeMap.setImageResource(R.drawable.map_type_hybrid)
        }
        else{
            map.mapType = GoogleMap.MAP_TYPE_HYBRID
            ivTypeMap.setImageResource(R.drawable.map_type_normal)
        }
    }

    fun callCenterMap(v: View){
        mapCentered = true
        if (latitude == 0.0) centerMap(init_lt, init_ln)
        else centerMap(latitude, longitude)
    }

    fun callShowHideMap(v: View){
        if (allPermissionsGrantedGPS()){
            var lyMap = findViewById<LinearLayout>(R.id.lyMap)
            var lyFragmentMap = findViewById<LinearLayout>(R.id.lyFragmentMap)
            var ivOpenClose = findViewById<ImageView>(R.id.ivOpenClose)

            if (lyMap.height == 0){
                setHeightLinearLayout(lyMap, 1157)
                animateViewofFloat(lyFragmentMap, "translationY", 0f, 0)
                ivOpenClose.rotation = 180f
            }
            else{
                setHeightLinearLayout(lyMap, 0)
                lyFragmentMap.translationY= -300f
                ivOpenClose.rotation = 0f
            }

        }
        else requestPermissionLocation()
    }

    private fun initPermissionsGPS(){
        if (allPermissionsGrantedGPS())
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        else
            requestPermissionLocation()
    }

    private fun requestPermissionLocation(){
        ActivityCompat.requestPermissions(this, arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION), PERMISSION_ID)
    }

    private fun allPermissionsGrantedGPS() = REQUIRED_PERMISSIONS_GPS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun isLocationEnabled(): Boolean{
        var locationManager: LocationManager
                = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

    }

    private fun activationLocation(){
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        startActivity(intent)
    }

    private fun checkPermission(): Boolean{
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
    }

    private fun manageLocation(){
        if (checkPermission()){

            if (isLocationEnabled()){
                if (ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_FINE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED
                    &&  ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_COARSE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED) {


                    fusedLocationClient.lastLocation.addOnSuccessListener {
                        requestNewLocationData()
                    }
                }
            }
            else activationLocation()
        }
        else requestPermissionLocation()
    }

    @SuppressLint("MissingPermission")
    private fun requestNewLocationData(){
        var mLocationRequest = com.google.android.gms.location.LocationRequest()
        mLocationRequest.priority = PRIORITY_HIGH_ACCURACY
        mLocationRequest.interval = 0
        mLocationRequest.fastestInterval = 0
        mLocationRequest.numUpdates = 1
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        fusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallBack, Looper.myLooper())


    }

    private val mLocationCallBack = object: LocationCallback(){
        override fun onLocationResult(locationResult: LocationResult) {
            var mLastLocation : Location = locationResult.lastLocation!!

            init_lt = mLastLocation.latitude
            init_ln = mLastLocation.longitude

            if (timeInSeconds > 0L) registerNewLocation(mLastLocation)
        }
    }

    private fun registerNewLocation(location: Location){
        var new_latitude: Double = location.latitude
        var new_longitude: Double = location.longitude

        if (flagSavedLocation){
            if (timeInSeconds >= INTERVAL_LOCATION){
                var distanceInterval = calculateDistance(new_latitude, new_longitude)

                if ( distanceInterval <= LIMIT_DISTANCE_ACCEPTED){
                    updateSpeeds(distanceInterval)
                    refreshInterfaceData()

                    saveLocation(location)

                    var newPos = LatLng (new_latitude, new_longitude)
                    (listPoints as ArrayList<LatLng>).add(newPos)
                    createPolylines(listPoints)

                }

            }
        }
        latitude = new_latitude
        longitude = new_longitude

        if (mapCentered == true) centerMap(latitude, longitude)

        if (minLatitude == null){
            minLatitude = latitude
            maxLatitude = latitude
            minLongitude = longitude
            maxLongitude = longitude
        }
        if (latitude < minLatitude!!) minLatitude = latitude
        if (latitude > maxLatitude!!) maxLatitude = latitude
        if (longitude < minLongitude!!) minLongitude = longitude
        if (longitude > maxLongitude!!) maxLongitude = longitude

        if (location.hasAltitude()){
            if (maxAltitude == null){
                maxAltitude = location.altitude
                minAltitude = location.altitude
            }
            if (location.latitude > maxAltitude!!) maxAltitude = location.altitude
            if (location.latitude < minAltitude!!) minAltitude = location.altitude
        }

    }

    private fun checkMedals(d: Double, aS: Double, mS: Double){
        if (d>0){
            if (d >= medalsListSportSelectedDistance.get(0)){
                recDistanceGold = true; recDistanceSilver = false; recDistanceBronze = false
                notifyMedal("distance", "gold", "PERSONAL")
            }
            else{
                if (d >= medalsListSportSelectedDistance.get(1)){
                    recDistanceGold = false; recDistanceSilver = true; recDistanceBronze = false
                    notifyMedal("distance", "silver", "PERSONAL")
                }
                else{
                    if (d >= medalsListSportSelectedDistance.get(2)){
                        recDistanceGold = false; recDistanceSilver = false; recDistanceBronze = true
                        notifyMedal("distance", "bronze", "PERSONAL")
                    }
                }
            }
        }

        if (aS > 0){
            if (aS >= medalsListSportSelectedAvgSpeed.get(0)){
                recAvgSpeedGold = true; recAvgSpeedSilver = false; recAvgSpeedBronze = false
                notifyMedal("avgSpeed", "gold", "PERSONAL")
            }
            else{
                if (aS >= medalsListSportSelectedAvgSpeed.get(1)){
                    recAvgSpeedGold = false; recAvgSpeedSilver = true; recAvgSpeedBronze = false
                    notifyMedal("avgSpeed", "silver", "PERSONAL")
                }
                else{
                    if (aS >= medalsListSportSelectedAvgSpeed.get(2)){
                        recAvgSpeedGold = false; recAvgSpeedSilver = false; recAvgSpeedBronze = true
                        notifyMedal("avgSpeed", "bronze", "PERSONAL")
                    }
                }
            }
        }

        if (mS > 0){
            if (mS >= medalsListSportSelectedMaxSpeed.get(0)){
                recMaxSpeedGold = true; recMaxSpeedSilver = false; recMaxSpeedBronze = false
                notifyMedal("maxSpeed", "gold", "PERSONAL")
            }
            else{
                if (mS >= medalsListSportSelectedMaxSpeed.get(1)){
                    recMaxSpeedGold = false; recMaxSpeedSilver = true; recMaxSpeedBronze = false
                    notifyMedal("maxSpeed", "silver", "PERSONAL")
                }
                else{
                    if (mS >= medalsListSportSelectedMaxSpeed.get(2)){
                        recMaxSpeedGold = false; recMaxSpeedSilver = false; recMaxSpeedBronze = true
                        notifyMedal("maxSpeed", "bronze", "PERSONAL")
                    }
                }
            }
        }

    }

    private fun notifyMedal(category: String, metal: String, scope: String){
        var CHANNEL_ID = "NEW $scope RECORD - $sportSelected"

        var textNotification =""
        when (metal){
            "gold"-> textNotification = "1ª "
            "silver"-> textNotification = "2ª "
            "bronze"-> textNotification = "3ª "
        }
        textNotification += "mejor marca personal en "
        when (category){
            "distance"-> textNotification += "distancia recorrida"
            "avgSpeed"-> textNotification += " velocidad promedio"
            "maxSpeed"-> textNotification += " velocidad máxima alcanzada"
        }

        var iconNotificacion: Int = 0
        when (metal){
            "gold" -> iconNotificacion = R.drawable.medalgold
            "silver"-> iconNotificacion = R.drawable.medalsilver
            "bronze"-> iconNotificacion = R.drawable.medalbronze
        }

        var builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(iconNotificacion)
            .setContentTitle(CHANNEL_ID)
            .setContentText(textNotification)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)


        var notificationId: Int = 0
        when (category){
            "distance"->
                when (metal){
                    "gold"->notificationId = 11
                    "silver"->notificationId = 12
                    "bronze"->notificationId = 13
                }
            "avgSpeed"->
                when (metal){
                    "gold"->notificationId = 21
                    "silver"->notificationId = 22
                    "bronze"->notificationId = 23
                }
            "maxSpeed"->
                when (metal){
                    "gold"->notificationId = 31
                    "silver"->notificationId = 32
                    "bronze"->notificationId = 33
                }
        }

        with(NotificationManagerCompat.from(this)) {
            // notificationId is a unique int for each notification that you must define
          /*  notify(notificationId, builder.build())*/
        }

    }




    private fun calculateDistance(n_lt: Double, n_lg: Double): Double{
        val radioTierra = 6371.0 //en kilómetros

        val dLat = Math.toRadians(n_lt - latitude)
        val dLng = Math.toRadians(n_lg - longitude)
        val sindLat = Math.sin(dLat / 2)
        val sindLng = Math.sin(dLng / 2)
        val va1 =
            Math.pow(sindLat, 2.0) + (Math.pow(sindLng, 2.0)
                    * Math.cos(Math.toRadians(latitude)) * Math.cos(
                Math.toRadians( n_lt  )
            ))
        val va2 = 2 * Math.atan2(Math.sqrt(va1), Math.sqrt(1 - va1))
        var n_distance =  radioTierra * va2

        if (n_distance < LIMIT_DISTANCE_ACCEPTED) distance += n_distance

        return n_distance
    }

    private fun updateSpeeds(d: Double) {
        //la distancia se calcula en km, asi que la pasamos a metros para el calculo de velocidadr
        //convertirmos m/s a km/h multiplicando por 3.6
        speed = ((d * 1000) / INTERVAL_LOCATION) * 3.6
        if (speed > maxSpeed) maxSpeed = speed
        avgSpeed = ((distance * 1000) / timeInSeconds) * 3.6
    }

    private fun refreshInterfaceData(){
        var tvCurrentDistance = findViewById<TextView>(R.id.tvCurrentDistance)
        var tvCurrentAvgSpeed = findViewById<TextView>(R.id.tvCurrentAvgSpeed)
        var tvCurrentSpeed = findViewById<TextView>(R.id.tvCurrentSpeed)
        tvCurrentDistance.text = roundNumber(distance.toString(), 2)
        tvCurrentAvgSpeed.text = roundNumber(avgSpeed.toString(), 1)
        tvCurrentSpeed.text = roundNumber(speed.toString(), 1)


        csbCurrentDistance.progress = distance.toFloat()
        if (distance > totalsSelectedSport.recordDistance!!){
            tvDistanceRecord.text = roundNumber(distance.toString(), 1)
            tvDistanceRecord.setTextColor(ContextCompat.getColor(this, R.color.salmon_dark))

            csbCurrentDistance.max = distance.toFloat()
            csbCurrentDistance.progress = distance.toFloat()

            totalsSelectedSport.recordDistance = distance
        }

        csbCurrentAvgSpeed.progress = avgSpeed.toFloat()
        if (avgSpeed > totalsSelectedSport.recordAvgSpeed!!){
            tvAvgSpeedRecord.text = roundNumber(avgSpeed.toString(), 1)
            tvAvgSpeedRecord.setTextColor(ContextCompat.getColor(this, R.color.salmon_dark))

            csbRecordAvgSpeed.max = avgSpeed.toFloat()
            csbRecordAvgSpeed.progress = avgSpeed.toFloat()
            csbCurrentAvgSpeed.max = avgSpeed.toFloat()

            totalsSelectedSport.recordAvgSpeed = avgSpeed
        }


        if (speed > totalsSelectedSport.recordSpeed!!){
            tvMaxSpeedRecord.text = roundNumber(speed.toString(), 1)
            tvMaxSpeedRecord.setTextColor(ContextCompat.getColor(this, R.color.salmon_dark))

            csbRecordSpeed.max = speed.toFloat()
            csbRecordSpeed.progress = speed.toFloat()


            csbCurrentMaxSpeed.max = speed.toFloat()
            csbCurrentMaxSpeed.progress = speed.toFloat()

            csbCurrentSpeed.max = speed.toFloat()

            totalsSelectedSport.recordSpeed = speed
        }
        else{
            if (speed == maxSpeed){
                csbCurrentMaxSpeed.max = csbRecordSpeed.max
                csbCurrentMaxSpeed.progress = speed.toFloat()

                csbCurrentSpeed.max = csbRecordSpeed.max
            }
        }
        csbCurrentSpeed.progress = speed.toFloat()


    }

    private fun createPolylines(listPosition: Iterable<LatLng>){
        val polylineOptions = PolylineOptions()
            .width(25f)
            .color(ContextCompat.getColor(this, R.color.salmon_dark))
            .addAll(listPosition)

        val polyline = map.addPolyline(polylineOptions)
        polyline.startCap = RoundCap()

    }

    private fun saveLocation(location: Location){
        var dirName = dateRun + startTimeRun
        dirName = dirName.replace("/", "")
        dirName = dirName.replace(":", "")

        var docName = timeInSeconds.toString()
        while (docName.length < 4) docName = "0" + docName

        var ms: Boolean
        ms = speed == maxSpeed && speed > 0


        var dbLocation = FirebaseFirestore.getInstance()
        dbLocation.collection("locations/$userEmail/$dirName").document(docName).set(hashMapOf(
            "time" to SimpleDateFormat("HH:mm:ss").format(Date()),
            "latitude" to location.latitude,
            "longitude" to location.longitude,
            "altitude" to location.altitude,
            "hasAltitude" to location.hasAltitude(),
            "speedFromGoogle" to location.speed,
            "speedFromMe" to speed,
            "maxSpeed" to ms,
            "color" to tvChrono.currentTextColor
        ))

    }

    /*fun selectBike(v: View){
        if (timeInSeconds.toInt() == 0) selectSport("Bike")
    }
    fun selectRollerSkate(v: View){
        if (timeInSeconds.toInt() == 0) selectSport("RollerSkate")
    }*/

    fun selectRunning(v: View){
        if (timeInSeconds.toInt() == 0) selectSport("Running")
    }

    private fun selectSport(sport: String){

        sportSelected = sport

       /* var lySportBike = findViewById<LinearLayout>(R.id.lySportBike)
        var lySportRollerSkate = findViewById<LinearLayout>(R.id.lySportRollerSkate)*/
        var lySportRunning = findViewById<LinearLayout>(R.id.lySportRunning)

        when (sport){
            /*"Bike"->{
                LIMIT_DISTANCE_ACCEPTED = LIMIT_DISTANCE_ACCEPTED_BIKE

                lySportBike.setBackgroundColor(ContextCompat.getColor(mainContext, R.color.orange))
                lySportRollerSkate.setBackgroundColor(ContextCompat.getColor(mainContext, R.color.gray_medium))
                lySportRunning.setBackgroundColor(ContextCompat.getColor(mainContext, R.color.gray_medium))
            }
            "RollerSkate"->{
                LIMIT_DISTANCE_ACCEPTED = LIMIT_DISTANCE_ACCEPTED_ROLLERSKATE

                lySportBike.setBackgroundColor(ContextCompat.getColor(mainContext, R.color.gray_medium))
                lySportRollerSkate.setBackgroundColor(ContextCompat.getColor(mainContext, R.color.orange))
                lySportRunning.setBackgroundColor(ContextCompat.getColor(mainContext, R.color.gray_medium))
            }*/
            "Running"->{
                LIMIT_DISTANCE_ACCEPTED = LIMIT_DISTANCE_ACCEPTED_RUNNING

               /* lySportBike.setBackgroundColor(ContextCompat.getColor(mainContext, R.color.gray_medium))
                lySportRollerSkate.setBackgroundColor(ContextCompat.getColor(mainContext, R.color.gray_medium))*/
                lySportRunning.setBackgroundColor(ContextCompat.getColor(mainContext, R.color.orange))

                levelSelectedSport = levelRunning
                totalsSelectedSport = totalsRunning

                medalsListSportSelectedDistance = medalsListRunningDistance
                medalsListSportSelectedAvgSpeed = medalsListRunningAvgSpeed
                medalsListSportSelectedMaxSpeed = medalsListRunningMaxSpeed

            }
        }
        refreshCBSsSport()
        refreshRecords()
    }

    private fun refreshCBSsSport(){
        csbRecordDistance.max = totalsSelectedSport.recordDistance?.toFloat()!!
        csbRecordDistance.progress = totalsSelectedSport.recordDistance?.toFloat()!!

        csbRecordAvgSpeed.max = totalsSelectedSport.recordAvgSpeed?.toFloat()!!
        csbRecordAvgSpeed.progress = totalsSelectedSport.recordAvgSpeed?.toFloat()!!

        csbRecordSpeed.max = totalsSelectedSport.recordSpeed?.toFloat()!!
        csbRecordSpeed.progress = totalsSelectedSport.recordSpeed?.toFloat()!!

        csbCurrentDistance.max = csbRecordDistance.max
        csbCurrentAvgSpeed.max = csbRecordAvgSpeed.max
        csbCurrentSpeed.max = csbRecordSpeed.max
        csbCurrentMaxSpeed.max = csbRecordSpeed.max
        csbCurrentMaxSpeed.progress = 0f

    }
    private fun refreshRecords(){
        if (totalsSelectedSport.recordDistance!! > 0)
            tvDistanceRecord.text = totalsSelectedSport.recordDistance.toString()
        else
            tvDistanceRecord.text = ""
        if (totalsSelectedSport.recordAvgSpeed!! > 0)
            tvAvgSpeedRecord.text = totalsSelectedSport.recordAvgSpeed.toString()
        else
            tvAvgSpeedRecord.text = ""
        if (totalsSelectedSport.recordSpeed!! > 0)
            tvMaxSpeedRecord.text = totalsSelectedSport.recordSpeed.toString()
        else
            tvMaxSpeedRecord.text = ""
    }

    private fun updateTotalsUser(){
        totalsSelectedSport.totalRuns = totalsSelectedSport.totalRuns!! + 1
        totalsSelectedSport.totalDistance = totalsSelectedSport.totalDistance!! + distance
        totalsSelectedSport.totalTime = totalsSelectedSport.totalTime!! + timeInSeconds.toInt()

        if (distance > totalsSelectedSport.recordDistance!!){
            totalsSelectedSport.recordDistance = distance
        }
        if (maxSpeed > totalsSelectedSport.recordSpeed!!){
            totalsSelectedSport.recordSpeed = maxSpeed
        }
        if (avgSpeed > totalsSelectedSport.recordAvgSpeed!!){
            totalsSelectedSport.recordAvgSpeed = avgSpeed
        }

        totalsSelectedSport.totalDistance = roundNumber(totalsSelectedSport.totalDistance.toString(),1).toDouble()
        totalsSelectedSport.recordDistance = roundNumber(totalsSelectedSport.recordDistance.toString(),1).toDouble()
        totalsSelectedSport.recordSpeed = roundNumber(totalsSelectedSport.recordSpeed.toString(),1).toDouble()
        totalsSelectedSport.recordAvgSpeed = roundNumber(totalsSelectedSport.recordAvgSpeed.toString(),1).toDouble()

        var collection = "totals$sportSelected"
        var dbUpdateTotals = FirebaseFirestore.getInstance()
        dbUpdateTotals.collection(collection).document(userEmail)
            .update("recordAvgSpeed", totalsSelectedSport.recordAvgSpeed)
        dbUpdateTotals.collection(collection).document(userEmail)
            .update("recordDistance", totalsSelectedSport.recordDistance)
        dbUpdateTotals.collection(collection).document(userEmail)
            .update("recordSpeed", totalsSelectedSport.recordSpeed)
        dbUpdateTotals.collection(collection).document(userEmail)
            .update("totalDistance", totalsSelectedSport.totalDistance)
        dbUpdateTotals.collection(collection).document(userEmail)
            .update("totalRuns", totalsSelectedSport.totalRuns)
        dbUpdateTotals.collection(collection).document(userEmail)
            .update("totalTime", totalsSelectedSport.totalTime)

        when (sportSelected){
            /*"Bike" -> {
                totalsBike = totalsSelectedSport
            }
            "RollerSkate" -> {
                totalsRollerSkate = totalsSelectedSport
            }*/
            "Running" -> {
                totalsRunning = totalsSelectedSport
            }
        }
    }


    fun startOrStopButtonClicked (v: View){
        manageStartStop()
    }

    private fun manageStartStop(){
        if (timeInSeconds == 0L && isLocationEnabled() == false){
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.alertActivationGPSTitle))
                .setMessage(getString(R.string.alertActivationGPSDescription))
                .setPositiveButton(R.string.aceptActivationGPS,
                    DialogInterface.OnClickListener { dialog, which ->
                        activationLocation()
                    })
                .setNegativeButton(R.string.ignoreActivationGPS,
                    DialogInterface.OnClickListener { dialog, which ->
                        activatedGPS = false
                        manageRun()
                    })
                .setCancelable(true)
                .show()
        }
        else manageRun()
    }

    private fun manageRun(){

        if (timeInSeconds.toInt() == 0){

            dateRun = SimpleDateFormat("yyyy/MM/dd").format(Date())
            startTimeRun = SimpleDateFormat("HH:mm:ss").format(Date())

            fbCamara.isVisible = true

            swIntervalMode.isClickable = false
            npDurationInterval.isEnabled = false
            csbRunWalk.isEnabled = false

            swChallenges.isClickable = false
            npChallengeDistance.isEnabled = false
            npChallengeDurationHH.isEnabled = false
            npChallengeDurationMM.isEnabled = false
            npChallengeDurationSS.isEnabled = false

            tvChrono.setTextColor(ContextCompat.getColor(this, R.color.chrono_running))

            mpHard?.start()
            controlRunningWalking = true

            if (activatedGPS) {
                flagSavedLocation = false
                manageLocation()
                flagSavedLocation = true
                manageLocation()
            }

        }
        if (!startButtonClicked){
            startButtonClicked = true
            startTime()
            manageEnableButtonsRun(false, true)

         /*   if (tvChrono.getCurrentTextColor() == ContextCompat.getColor(this, R.color.chrono_running))
                mpHard?.start()
            if (tvChrono.getCurrentTextColor() == ContextCompat.getColor(this, R.color.chrono_walking))
                mpSoft?.start()*/

            if (controlRunningWalking)
                mpHard?.start()
            else
                mpSoft?.start()
        }
        else{
            startButtonClicked = false
            stopTime()
            manageEnableButtonsRun(true, true)

          /*  if (tvChrono.getCurrentTextColor() == ContextCompat.getColor(this, R.color.chrono_running))
                mpHard?.pause()
            if (tvChrono.getCurrentTextColor() == ContextCompat.getColor(this, R.color.chrono_walking))
                mpSoft?.pause()*/

            if (controlRunningWalking)
                mpHard?.pause()
            else
                mpSoft?.pause()

        }
    }

    private fun manageEnableButtonsRun(e_reset: Boolean, e_run: Boolean){
        val tvReset = findViewById<TextView>(R.id.tvReset)
        val btStart = findViewById<LinearLayout>(R.id.btStart)
        val btStartLabel = findViewById<TextView>(R.id.btStartLabel)
        tvReset.isEnabled = e_reset
        btStart.isEnabled = e_run

        if (e_reset){
            tvReset.setBackgroundColor(ContextCompat.getColor(this, R.color.green))
            animateViewofFloat(tvReset, "translationY", 0f, 500)
        }
        else{
            tvReset.setBackgroundColor(ContextCompat.getColor(this, R.color.gray))
            animateViewofFloat(tvReset, "translationY", 150f, 500)
        }

        if (e_run){
            if (startButtonClicked){
                btStart.background = getDrawable(R.drawable.circle_background_topause)
                btStartLabel.setText(R.string.stop)
            }
            else{
                btStart.background = getDrawable(R.drawable.circle_background_toplay)
                btStartLabel.setText(R.string.start)
            }
        }
        else btStart.background = getDrawable(R.drawable.circle_background_todisable)


    }

    private fun startTime(){
        mHandler = Handler(Looper.getMainLooper())
        chronometer.run()
    }

    private fun stopTime(){
        mHandler?.removeCallbacks(chronometer)
    }

    private var chronometer: Runnable = object : Runnable {
        override fun run() {
            try{

                if (mpHard!!.isPlaying){
                    val sbHardTrack: SeekBar = findViewById(R.id.sbHardTrack)
                    sbHardTrack.progress = mpHard!!.currentPosition
                }
                if (mpSoft!!.isPlaying){
                    val sbSoftTrack: SeekBar = findViewById(R.id.sbSoftTrack)
                    sbSoftTrack.progress = mpSoft!!.currentPosition
                }

                updateTimesTrack(true, true)

                if (activatedGPS && timeInSeconds.toInt() % INTERVAL_LOCATION == 0) manageLocation()

                if (swIntervalMode.isChecked){
                    checkStopRun(timeInSeconds)
                    checkNewRound(timeInSeconds)
                }

                timeInSeconds += 1
                updateStopWatchView()
            } finally {
                mHandler!!.postDelayed(this, mInterval.toLong())
            }
        }
    }

    private fun updateStopWatchView(){
        tvChrono.text = getFormattedStopWatch(timeInSeconds * 1000)
    }

    private fun resetClicked(){

        /*savePreferences()*/
        saveDataRun()

        updateTotalsUser()
        setLevelSport(sportSelected)

        showPopUp()

        resetTimeView()
        resetInterface()
     //   resetMedals()
    }


    private fun saveDataRun(){

        var id:String = userEmail + dateRun + startTimeRun
        id = id.replace(":", "")
        id = id.replace("/", "")

        var saveDuration = tvChrono.text.toString()

        var saveDistance = roundNumber(distance.toString(),1)
        var saveAvgSpeed = roundNumber(avgSpeed.toString(),1)
        var saveMaxSpeed = roundNumber(maxSpeed.toString(),1)

        var centerLatitude = (minLatitude!! + maxLatitude!!) / 2
        var centerLongitude = (minLongitude!! + maxLongitude!!) / 2

        var collection = "runsRunning"
        var dbRun = FirebaseFirestore.getInstance()
        dbRun.collection(collection).document(id).set(hashMapOf(
            "user" to userEmail,
            "date" to dateRun,
            "startTime" to startTimeRun,
            "sport" to sportSelected,
            "activatedGPS" to activatedGPS,
            "duration" to saveDuration,
            "distance" to saveDistance.toDouble(),
            "avgSpeed" to saveAvgSpeed.toDouble(),
            "maxSpeed" to saveMaxSpeed.toDouble(),
            "minAltitude" to minAltitude,
            "maxAltitude" to maxAltitude,
            "minLatitude" to minLatitude,
            "maxLatitude" to maxLatitude,
            "minLongitude" to minLongitude,
            "maxLongitude" to maxLongitude,
            "centerLatitude" to centerLatitude,
            "centerLongitude" to centerLongitude
        ))

        if (swIntervalMode.isChecked){
            dbRun.collection(collection).document(id).update("intervalMode", true)
            dbRun.collection(collection).document(id).update("intervalDuration", npDurationInterval.value)
            dbRun.collection(collection).document(id).update("runningTime", tvRunningTime.text.toString())
            dbRun.collection(collection).document(id).update("walkingTime", tvWalkingTime.text.toString())
        }

        if (swChallenges.isChecked){
            if (challengeDistance > 0f)
                dbRun.collection(collection).document(id).update("challengeDistance", roundNumber(challengeDistance.toString(), 1).toDouble())
            if (challengeDuration > 0)
                dbRun.collection(collection).document(id).update("challengeDuration", getFormattedStopWatch(challengeDuration.toLong()))
        }

    }

    fun deleteRun(v:View){

        var id:String = userEmail + dateRun + startTimeRun
        id = id.replace(":", "")
        id = id.replace("/", "")

        var lyPopUpRun = findViewById<LinearLayout>(R.id.lyPopupRun)

        var currentRun = Runs()
        currentRun.distance = roundNumber(distance.toString(),1).toDouble()
        currentRun.avgSpeed = roundNumber(avgSpeed.toString(),1).toDouble()
        currentRun.maxSpeed = roundNumber(maxSpeed.toString(),1).toDouble()
        currentRun.duration = tvChrono.text.toString()

        deleteRunAndLinkedData(id, sportSelected, lyPopUpRun, currentRun)
        loadMedalsUser()
        setLevelSport(sportSelected)
        closePopUpRun()

    }

    private fun resetVariablesRun(){
        timeInSeconds = 0
        rounds = 1
        hardTime = true

        distance = 0.0
        maxSpeed = 0.0
        avgSpeed = 0.0

        minAltitude = null
        maxAltitude = null
        minLatitude = null
        maxLatitude = null
        minLongitude = null
        maxLongitude = null

        (listPoints as ArrayList<LatLng>).clear()

        challengeDistance = 0f
        challengeDuration = 0

        activatedGPS = true
        flagSavedLocation = false

    }

    private fun resetTimeView(){
        initStopWatch()
        manageEnableButtonsRun(false, true)

        //val btStart: LinearLayout = findViewById(R.id.btStart)
        //btStart.background = getDrawable(R.drawable.circle_background_toplay)
        tvChrono.setTextColor(ContextCompat.getColor(this, R.color.white))
    }

    private fun resetInterface(){

        fbCamara.isVisible = false

        val tvCurrentDistance: TextView = findViewById(R.id.tvCurrentDistance)
        val tvCurrentAvgSpeed: TextView = findViewById(R.id.tvCurrentAvgSpeed)
        val tvCurrentSpeed: TextView = findViewById(R.id.tvCurrentSpeed)
        tvCurrentDistance.text = "0.0"
        tvCurrentAvgSpeed.text = "0.0"
        tvCurrentSpeed.text = "0.0"


        tvDistanceRecord.setTextColor(ContextCompat.getColor(this, R.color.gray_dark))
        tvAvgSpeedRecord.setTextColor(ContextCompat.getColor(this, R.color.gray_dark))
        tvMaxSpeedRecord.setTextColor(ContextCompat.getColor(this, R.color.gray_dark))

        csbCurrentDistance.progress = 0f
        csbCurrentAvgSpeed.progress = 0f
        csbCurrentSpeed.progress = 0f
        csbCurrentMaxSpeed.progress = 0f

        val tvRounds: TextView = findViewById(R.id.tvRounds)
        tvRounds.text = getString(R.string.rounds)

        val lyChronoProgressBg = findViewById<LinearLayout>(R.id.lyChronoProgressBg)
        val lyRoundProgressBg = findViewById<LinearLayout>(R.id.lyRoundProgressBg)
        lyChronoProgressBg.translationX = -widthAnimations.toFloat()
        lyRoundProgressBg.translationX = -widthAnimations.toFloat()

        swIntervalMode.isClickable = true
        npDurationInterval.isEnabled = true
        csbRunWalk.isEnabled = true

        swChallenges.isClickable = true
        npChallengeDistance.isEnabled = true
        npChallengeDurationHH.isEnabled = true
        npChallengeDurationMM.isEnabled = true
        npChallengeDurationSS.isEnabled = true

        sbHardTrack.isEnabled = false
        sbSoftTrack.isEnabled = false
    }

    private fun updateProgressBarRound(secs: Long){
        var s = secs.toInt()
        while (s>=ROUND_INTERVAL) s-=ROUND_INTERVAL
        s++

        var lyRoundProgressBg = findViewById<LinearLayout>(R.id.lyRoundProgressBg)
        if (tvChrono.getCurrentTextColor() == ContextCompat.getColor(this, R.color.chrono_running)){

            var movement = -1 * (widthAnimations-(s*widthAnimations/TIME_RUNNING)).toFloat()
            animateViewofFloat(lyRoundProgressBg, "translationX", movement, 1000L)
        }
        if (tvChrono.getCurrentTextColor() == ContextCompat.getColor(this, R.color.chrono_walking)){
            s-= TIME_RUNNING
            var movement = -1 * (widthAnimations-(s*widthAnimations/(ROUND_INTERVAL-TIME_RUNNING))).toFloat()
            animateViewofFloat(lyRoundProgressBg, "translationX", movement, 1000L)
        }
    }

    private fun checkStopRun(Secs: Long){
        var secAux : Long = Secs
        while (secAux.toInt() > ROUND_INTERVAL) secAux -= ROUND_INTERVAL

        if (secAux.toInt() == TIME_RUNNING){
            tvChrono.setTextColor(ContextCompat.getColor(this, R.color.chrono_walking))

            val lyRoundProgressBg = findViewById<LinearLayout>(R.id.lyRoundProgressBg)
            lyRoundProgressBg.setBackgroundColor(ContextCompat.getColor(this, R.color.chrono_walking))
            lyRoundProgressBg.translationX = -widthAnimations.toFloat()

            mpHard?.pause()
            notifySound()
            mpSoft?.start()

            hardTime = false

        }
        else updateProgressBarRound(Secs)
    }

    private fun checkNewRound(Secs: Long){
        if (Secs.toInt() % ROUND_INTERVAL == 0 && Secs.toInt() > 0){
            val tvRounds: TextView = findViewById(R.id.tvRounds)
            rounds++
            tvRounds.text = "Round $rounds"

            tvChrono.setTextColor(ContextCompat.getColor( this, R.color.chrono_running))
            val lyRoundProgressBg = findViewById<LinearLayout>(R.id.lyRoundProgressBg)
            lyRoundProgressBg.setBackgroundColor(ContextCompat.getColor(this, R.color.chrono_running))
            lyRoundProgressBg.translationX = -widthAnimations.toFloat()

            mpSoft?.pause()
            notifySound()
            mpHard?.start()

            hardTime = true
        }
        else updateProgressBarRound(Secs)
    }

    private fun showPopUp(){
        var rlMain = findViewById<RelativeLayout>(R.id.rlMain)
        rlMain.isEnabled = false

        lyPopupRun.isVisible = true

        var lyWindow = findViewById<LinearLayout>(R.id.lyWindow)
        ObjectAnimator.ofFloat(lyWindow, "translationX", 0f ).apply {
            duration = 200L
            start()
        }

        loadDataPopUp()

    }

    private fun loadDataPopUp(){
        showHeaderPopUp()
        showMedals()
        showDataRun()
    }

    private fun showHeaderPopUp(){

        var csbRunsLevel = findViewById<CircularSeekBar>(R.id.csbRunsLevel)
        var csbDistanceLevel = findViewById<CircularSeekBar>(R.id.csbDistanceLevel)
        var tvTotalRunsLevel = findViewById<TextView>(R.id.tvTotalRunsLevel)
        var tvTotalDistanceLevel = findViewById<TextView>(R.id.tvTotalDistanceLevel)


        var ivSportSelected = findViewById<ImageView>(R.id.ivSportSelected)
        var ivCurrentLevel = findViewById<ImageView>(R.id.ivCurrentLevel)
        var tvTotalDistance = findViewById<TextView>(R.id.tvTotalDistance)
        var tvTotalTime = findViewById<TextView>(R.id.tvTotalTime)

        when (sportSelected){
            /*"Bike" ->{
                levelSelectedSport = levelBike
                setLevelBike()
                ivSportSelected.setImageResource(R.mipmap.bike)
            }
            "RollerSkate" -> {
                levelSelectedSport = levelRollerSkate
                setLevelRollerSkate()
                ivSportSelected.setImageResource(R.mipmap.rollerskate)
            }*/
            "Running" -> {
                levelSelectedSport = levelRunning
                setLevelRunning()
                ivSportSelected.setImageResource(R.mipmap.appshoes)
            }
        }

        var tvNumberLevel = findViewById<TextView>(R.id.tvNumberLevel)
        var levelText = "${getString(R.string.level)} ${levelSelectedSport.image!!.subSequence(6,7).toString()}"
        tvNumberLevel.text = levelText

        csbRunsLevel.max = levelSelectedSport.RunsTarget!!.toFloat()
        csbRunsLevel.progress = totalsSelectedSport.totalRuns!!.toFloat()
        if (totalsSelectedSport.totalRuns!! > levelSelectedSport.RunsTarget!!.toInt()){
            csbRunsLevel.max = levelSelectedSport.RunsTarget!!.toFloat()
            csbRunsLevel.progress = csbRunsLevel.max
        }

        csbDistanceLevel.max = levelSelectedSport.DistanceTarget!!.toFloat()
        csbDistanceLevel.progress = totalsSelectedSport.totalDistance!!.toFloat()
        if (totalsSelectedSport.totalDistance!! > levelSelectedSport.DistanceTarget!!.toInt()){
            csbDistanceLevel.max = levelSelectedSport.DistanceTarget!!.toFloat()
            csbDistanceLevel.progress = csbDistanceLevel.max
        }

        tvTotalRunsLevel.text = "${totalsSelectedSport.totalRuns!!}/${levelSelectedSport.RunsTarget!!}"

        var td = totalsSelectedSport.totalDistance!!
        var td_k: String = td.toString()
        if (td > 1000) td_k = (td/1000).toInt().toString() + "K"
        var ld = levelSelectedSport.DistanceTarget!!.toDouble()
        var ld_k: String = ld.toInt().toString()
        if (ld > 1000) ld_k = (ld/1000).toInt().toString() + "K"
        tvTotalDistance.text = "${td_k}/${ld_k} kms"

        var porcent = (totalsSelectedSport.totalDistance!!.toDouble() *100 / levelSelectedSport.DistanceTarget!!.toDouble()).toInt()
        tvTotalDistanceLevel.text = "$porcent%"

        when (levelSelectedSport.image){
            "level_1" -> ivCurrentLevel.setImageResource(R.drawable.level_1)
            "level_2" -> ivCurrentLevel.setImageResource(R.drawable.level_2)
            "level_3" -> ivCurrentLevel.setImageResource(R.drawable.level_2)
            "level_4" -> ivCurrentLevel.setImageResource(R.drawable.level_2)
            "level_5" -> ivCurrentLevel.setImageResource(R.drawable.level_2)
            "level_6" -> ivCurrentLevel.setImageResource(R.drawable.level_2)
            "level_7" -> ivCurrentLevel.setImageResource(R.drawable.level_2)
        }

        var formatedTime = getFormattedTotalTime(totalsSelectedSport.totalTime!!.toLong())
        tvTotalTime.text = getString(R.string.PopUpTotalTime) + formatedTime
    }

    private fun showMedals(){

        val ivMedalDistance = findViewById<ImageView>(R.id.ivMedalDistance)
        val ivMedalAvgSpeed = findViewById<ImageView>(R.id.ivMedalAvgSpeed)
        val ivMedalMaxSpeed = findViewById<ImageView>(R.id.ivMedalMaxSpeed)

        val tvMedalDistanceTitle = findViewById<TextView>(R.id.tvMedalDistanceTitle)
        val tvMedalAvgSpeedTitle = findViewById<TextView>(R.id.tvMedalAvgSpeedTitle)
        val tvMedalMaxSpeedTitle = findViewById<TextView>(R.id.tvMedalMaxSpeedTitle)

        if (recDistanceGold) ivMedalDistance.setImageResource(R.drawable.medalgold)
        if (recDistanceSilver) ivMedalDistance.setImageResource(R.drawable.medalsilver)
        if (recDistanceBronze) ivMedalDistance.setImageResource(R.drawable.medalbronze)
        if (recDistanceGold || recDistanceSilver || recDistanceBronze)
            tvMedalDistanceTitle.setText(R.string.medalDistanceDescription)

        if (recAvgSpeedGold) ivMedalAvgSpeed.setImageResource(R.drawable.medalgold)
        if (recAvgSpeedSilver) ivMedalAvgSpeed.setImageResource(R.drawable.medalsilver)
        if (recAvgSpeedBronze) ivMedalAvgSpeed.setImageResource(R.drawable.medalbronze)
        if (recAvgSpeedGold || recAvgSpeedSilver || recAvgSpeedBronze)
            tvMedalAvgSpeedTitle.setText(R.string.medalAvgSpeedDescription)

        if (recMaxSpeedGold) ivMedalMaxSpeed.setImageResource(R.drawable.medalgold)
        if (recMaxSpeedSilver) ivMedalMaxSpeed.setImageResource(R.drawable.medalsilver)
        if (recMaxSpeedBronze) ivMedalMaxSpeed.setImageResource(R.drawable.medalbronze)
        if (recMaxSpeedGold || recMaxSpeedSilver || recMaxSpeedBronze)
            tvMedalMaxSpeedTitle.setText(R.string.medalMaxSpeedDescription)

    }

    private fun showDataRun(){
        var tvDurationRun = findViewById<TextView>(R.id.tvDurationRun)
        var lyChallengeDurationRun = findViewById<LinearLayout>(R.id.lyChallengeDurationRun)
        var tvChallengeDurationRun = findViewById<TextView>(R.id.tvChallengeDurationRun)
        var lyIntervalRun = findViewById<LinearLayout>(R.id.lyIntervalRun)
        var tvIntervalRun = findViewById<TextView>(R.id.tvIntervalRun)
        var tvDistanceRun = findViewById<TextView>(R.id.tvDistanceRun)
        var lyChallengeDistancePopUp = findViewById<LinearLayout>(R.id.lyChallengeDistancePopUp)
        var tvChallengeDistanceRun = findViewById<TextView>(R.id.tvChallengeDistanceRun)
        var lyUnevennessRun = findViewById<LinearLayout>(R.id.lyUnevennessRun)
        var tvMaxUnevennessRun = findViewById<TextView>(R.id.tvMaxUnevennessRun)
        var tvMinUnevennessRun = findViewById<TextView>(R.id.tvMinUnevennessRun)
        var tvAvgSpeedRun = findViewById<TextView>(R.id.tvAvgSpeedRun)
        var tvMaxSpeedRun = findViewById<TextView>(R.id.tvMaxSpeedRun)

        tvDurationRun.setText(tvChrono.text)
        if (challengeDuration > 0){
            setHeightLinearLayout(lyChallengeDurationRun, 120)
            tvChallengeDurationRun.setText(getFormattedStopWatch((challengeDuration*1000).toLong()))
        }
        else  setHeightLinearLayout(lyChallengeDurationRun, 0)

        if (swIntervalMode.isChecked){
            setHeightLinearLayout(lyIntervalRun, 120)
            var details: String = "${npDurationInterval.value}mins. ("
            details += "${tvRunningTime.text} / ${tvWalkingTime.text})"

            tvIntervalRun.setText(details)
        }
        else setHeightLinearLayout(lyIntervalRun, 0)

        tvDistanceRun.setText(roundNumber(distance.toString(), 2))
        if (challengeDistance > 0f){
            setHeightLinearLayout(lyChallengeDistancePopUp, 120)
            tvChallengeDistanceRun.setText(challengeDistance.toString())
        }
        else setHeightLinearLayout(lyChallengeDistancePopUp, 0)

        if (maxAltitude == null) setHeightLinearLayout(lyUnevennessRun, 0)
        else{
            setHeightLinearLayout(lyUnevennessRun, 120)
            tvMaxUnevennessRun.setText(maxAltitude!!.toInt().toString())
            tvMinUnevennessRun.setText(minAltitude!!.toInt().toString())
        }

        tvAvgSpeedRun.setText(roundNumber(avgSpeed.toString(), 1))
        tvMaxSpeedRun.setText(roundNumber(maxSpeed.toString(), 1))

    }


    fun closePopUp (v: View){
        closePopUpRun()
    }

    private fun closePopUpRun(){
        hidePopUpRun()
        var rlMain = findViewById<RelativeLayout>(R.id.rlMain)
        rlMain.isEnabled = true

        resetVariablesRun()
        selectSport(sportSelected)
    }

    private fun hidePopUpRun(){
        var lyWindow = findViewById<LinearLayout>(R.id.lyWindow)
        lyWindow.translationX = 400f
        lyPopupRun = findViewById(R.id.lyPopupRun)
        lyPopupRun.isVisible = false
    }

}