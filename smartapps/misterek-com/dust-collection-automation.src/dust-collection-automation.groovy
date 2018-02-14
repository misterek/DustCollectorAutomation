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
        input "meters", "capability.powerMeter", required:true, title:"Tools to Monitor", multiple:true
        input "dustcollector", "capability.switch", required:true, title:"Dust Collector"
        paragraph "Delay is how long you want the dust collector to stay on after all tools have been turned off.  This allows you to keep the dust collector from turing on and off too much."
        input name: "delay", type:"number", title: "Delay", defaultValue: "5"
        paragraph "Threshold is the amount of power (in watts) you want a tool to draw to be considered on.  Anything under this will be considered off.  Anything over will be considered on."
        input name: "threshold", type:"number", title: "Threshold", defaultValue: "50"
        

	}        
}

def installed() {
	//log.debug "Installed with settings: ${settings}"
    state.powerMap = [:]
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
    for (meter in meters){
        state.powerMap[meter.id] = [:]
        state.powerMap[meter.id]["date"] = null
        state.powerMap[meter.id]["previousPower"] = 0
    }
    subscribe(meters, "power", meterHandler)    
}

def meterHandler(evt){    
    
  	checklights()

}

def secondsAgo(seconds){
    Calendar cal = Calendar.getInstance();
    cal.setTime(new Date());  
    cal.add(Calendar.SECOND, seconds * -1);
    return cal.getTime();   
}

def checklights(){
    
    def toolOn = false
    def toolTurnedOff = false

    for (meter in meters){
        if (meter.currentPower > threshold){
            toolOn = true
        }
        
        // The tool turned off
        if( meter.currentPower <= threshold && state.powerMap[meter.id]["previousPower"] > threshold){
            toolTurnedOff = true
        }
        
        state.powerMap[meter.id]["previousPower"] = meter.currentPower
    }
    
   
    if ( toolOn == true){
        dustcollector.on()
        state.allOff = null
    }
    
    if ( toolOn == false && toolTurnedOff == true ){
        // set all off date
        state.allOff = new Date()
    }
    
    if ( toolOn == false /* And the dustcollector is on */ ) {
        if ( state.allOff != null){
            // Still not sure why, but allOff is becoming a string at times. So turn it back to a date.
            if (state.allOff instanceof String){
                state.allOff = Date.parse("yyyy-MM-dd'T'HH:mm:ssZ", state.allOff)
            }

            if ( state.allOff.before( secondsAgo(delay)) ){
                dustcollector.off()
                state.allOff = null             
            }else{
                runIn(1, checklights)
            }
        }
    }
    
    

}

