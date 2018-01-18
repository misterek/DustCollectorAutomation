/**
 *  TestWoodworking
 *
 *  Copyright 2017 Brad Misterek
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
import groovy.time.*
definition(
    name: "Dust Collection Automation",
    namespace: "misterek.com",
    author: "Brad Misterek",
    description: "Smartapp to Automate Dust Collection for a woodshop.  When any tool is turned on, the dust collector will turn on.  When all the tools are off, the dustcollector will turn off.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Dust Collection") {
        input "meters", "capability.powerMeter", required:true, title:"Tools to monitor", multiple:true
        input "dustcollector", "capability.switch", required:true, title:"Dust Collector"
        input name: "delay", type:"number", title: "Delay", defaultValue: "5"

	}        
}

def installed() {
	//log.debug "Installed with settings: ${settings}"
	initialize()
}

def updated() {
	//log.debug "Updated with settings: ${settings}"
	unsubscribe()
	initialize()
}

def initialize() {   
    //Subscribe to power updates on the meters
    state.delay = 0
    subscribe(meters, "power", meterHandler)    
}

def meterHandler(evt){    
    
    if( state.delay == 0 ){
    	checklights()
    }
}

def checklights(){
    log.debug "Check LIghts"
    state.delay = 0
    boolean allOff = true
    for (meter in meters){
        if (meter.currentPower > 10){
            dustcollector.on()
            
            // If anything is on, check again in 5 seconds
            state.delay = delay
            runIn(delay, checklights)
            
            //Don't remember what I was doing here
            //state.mydate = new Date()
            
            allOff = false
        }
    }
    
    if (allOff == true){
        dustcollector.off()
    }       
}

