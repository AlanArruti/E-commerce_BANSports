package com.BlandiArruti.E_commerce.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Habilita las tareas programadas (@Scheduled), como la consulta periódica del dólar. */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
