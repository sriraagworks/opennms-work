/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2024 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2024 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/


import org.codehaus.jackson.map.ObjectMapper
import groovy.util.logging.Slf4j
import org.opennms.features.openconfig.proto.gnmi.Gnmi
import org.opennms.netmgt.collection.api.AttributeType
import org.opennms.netmgt.collection.support.builder.InterfaceLevelResource
import org.opennms.netmgt.collection.support.builder.NodeLevelResource

@Slf4j
class CollectionSetGenerator {
    static generate(agent, builder, response) {
        log.info("Generating collection set for message: {}", response)
        NodeLevelResource nodeLevelResource = new NodeLevelResource(agent.getNodeId())
        // Sample code for parsing and building resources, real data may vary
        Gnmi.Notification notification = response.getUpdate()
        List<Gnmi.Update> updateList = notification.getUpdateList() // Getting the update {} from the payload
        String interfaceLabel = null;
        //log.info("STARTING COLLECTION FOR NODEID {}",nodeLevelResource.getNodeid())
        log.info("\n GETTING PREFIX ELEMENT COUNT: {}",notification.getPrefix().getElemCount()) // Getting the prefix{} from the payload

        // Working with PREFIX PAYLOAD
        for (Gnmi.PathElem pathElem : notification.getPrefix().getElemList()) {
            log.info("\n GETTING PREFIX ELEMENT LIST : {}", pathElem.getName())
            if (pathElem.getName().equals("interface")) {
                interfaceLabel = pathElem.getKeyMap().get("name");
                log.info("Value of interfaceLabel:: {}",interfaceLabel)
                break;
            }
        }

        log.info("\n OUTSIDE OF PREFIX PAYLOAD \n")
        if (interfaceLabel == null) {
            interfaceLabel = agent.getHostAddress();
        }


        // Creating InterfaceLevelResource Object
        InterfaceLevelResource interfaceResource = new InterfaceLevelResource(nodeLevelResource, interfaceLabel.replaceAll("/","_"));

        // Working on update{} in the payload
        for (Gnmi.Update update: updateList){
            log.info("\n**********************INSIDE OF UPDATE_PAYLOAD**********************")
            //log.info("\nUPDATE_PAYLOAD PATH: {}",update.getPath().getElemList()) // Either print the whole list or pass this list to pathelem to do getName()
            StringBuilder pathName = new StringBuilder();
            for (Gnmi.PathElem pathElem : update.getPath().getElemList()) {
                pathName.append(pathElem.getName()).append("/");
                //log.info("INSIDE FOR : pathName: {}", pathName.toString())
            }
            pathName.setLength(pathName.length() - 1); // Remove trailing slash
            log.info("PATH_NAME {} has PATH_VALUE {} for IF_LABEL {}", pathName.toString(), update.getVal(),interfaceLabel.replaceAll("/","_"))


        if (pathName.toString().matches(".*(octets|high-speed|oper-status).*")){
            if (update.getVal().getValueCase().equals(Gnmi.TypedValue.ValueCase.UINT_VAL)){
                // UINT_VAL
                log.info("INSIDE UINT_VAL")
                long value = update.getVal().getUintVal();
                log.info("VALUE FOR {} IS {}",pathName.toString(),value);
                builder.withNumericAttribute(interfaceResource, "gnmi-interfaces", pathName.toString().replaceAll("/","_"), value, AttributeType.COUNTER);
                log.info("PERSISTED UINT_VAL")

            }else if(update.getVal().getValueCase().equals(Gnmi.TypedValue.ValueCase.JSON_VAL)){
                // JSON_VAL
                log.info("INSIDE JSON_VAL")
                String local_path_name = pathName.toString();
                log.info("INSIDE JSON_VAL -> LOCAL-PATH-NAME: {} for IF_LABEL {}",local_path_name,interfaceLabel)
                if (local_path_name.matches(".*parent-ae-name.*")){
                    log.info("PARENT_AE_NAME VALUE WAS FOUND")
                }else{
                String value = update.getVal().getJsonVal().toStringUtf8();
                ObjectMapper objectMapper = new ObjectMapper();
                Long jsonValLong = objectMapper.readValue(value, Long.class);
                log.info("VALUE FOR {} IS {} OF TYPE {} for IF_LABEL {}",pathName.toString(),jsonValLong,jsonValLong.getClass().getCanonicalName(),interfaceLabel);
                builder.withNumericAttribute(interfaceResource, "gnmi-interfaces", pathName.toString().replaceAll("/","_"), jsonValLong, AttributeType.COUNTER);
                log.info("PERSISTED JSON_VAL")
                }
            }else{
                // catch-all to write builders to persist data of different types
                log.info("INSIDE ANY_VAL CATCH_ALL")
                log.info("VALUE FOR {} IS {} OF TYPE {} for IF_LABEL {}",pathName.toString(),update.getVal(),update.getVal().getClass().getCanonicalName(),interfaceLabel);

            }
        }
        else{
            log.info("PATH-NAME {} does not match for IF_LABEL {}",pathName.toString(),interfaceLabel)
        }
        }
    }
}

// The following variables are passed in as globals from the adapter:
// agent: the agent (or node) against which the metrics will be associated
// builder: a reference to a CollectionSetBuilder to which the resources/metrics should be added
// msg: the message from which to extract the metrics

Gnmi.SubscribeResponse response = msg

// Generate the CollectionSet
CollectionSetGenerator.generate(agent, builder, response)



















/*
def int i = 2
def int[] arr = [1,2,3,4,5]
    println("\n")

for ( int j in arr){
    println("J VALUE is ${j}")
    println("I VALUE is ${i}")
    if ( i == j){
        println("inside break")
        break;
    }else{
        println("inside continue")
        continue;
    }
    
}
*/