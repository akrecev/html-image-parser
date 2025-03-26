/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ask.config.sequence;

/**
 *
 * @author Marina
 */
public interface ServiceIdGen_I {
    long getNextVal(String entityName) throws RuntimeException;
}
