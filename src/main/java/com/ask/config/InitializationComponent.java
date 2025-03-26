/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ask.config;


import com.ask.config.monitor.MonitorErrorsService;
import com.ask.config.sequence.ServiceIdGenPostgre;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author Marina
 */
@Component
public class InitializationComponent {

    private final ServiceIdGenPostgre serviceId;
    private final MonitorErrorsService serviceHealth;

    @Autowired
    public InitializationComponent(ServiceIdGenPostgre serviceId, MonitorErrorsService serviceHealth) {
        this.serviceId = serviceId;
        this.serviceHealth = serviceHealth;
    }

    @PostConstruct
    public void initialize() throws InterruptedException {

    }
}
