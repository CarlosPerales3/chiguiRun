package com.perales.chiguirun

data class Runs(
    var date: String ?= null,
    var starTime: String ?= null,
    var user: String ?= null,
    var duration: String ?= null,

    var intervalMode: Boolean ?= null,
    var intervalDuration: Int ?= null,
    var runningTime: String ?= null,
    var walkingTime: String ?= null,

    var challengeDuration: String ?= null,
    var challengeDistance: Boolean ?= null,

    var distance: Double ?= null,
    var maxSpeed: Double ?= null,
    var avgSpeed: Double ?= null,

    var maxAltitude: Double ?= null,
    var minAltitude: Double ?= null,
    var maxLatitude: Double ?= null,
    var minLatitude: Double ?= null,
    var maxLongitud: Double ?= null,
    var minLongitud: Double ?= null,

    var centerLatitude: Double ?= null,
    var centerLongitude: Double ?= null,

    var sport: String ?= null,

    var activatedGPS: Boolean ?= null,

    var medalDistance: String ?= null,
    var medalAvgSpeed: String ?= null,
    var medalMaxSpeed: String ?= null

    )
