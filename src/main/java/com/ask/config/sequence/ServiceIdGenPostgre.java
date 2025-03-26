/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ask.config.sequence;

import com.ask.exception.Except4Support;
import com.ask.exception.Except4SupportDocumented;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;
import org.springframework.stereotype.Service;

/**
 *
 * @author Marina
 */
@Service
public class ServiceIdGenPostgre implements ServiceIdGen_I {

    @PersistenceContext
    private EntityManager entityManager;
    private static TreeMap<String, SequenceCache> sequences = new TreeMap<>();
    private static TreeSet<String> sequencesNames = new TreeSet<>();

    public void register(String entityName, String sequenceName, int step) {
            // only for checking programmers
        if(sequences.containsKey(entityName))
            throw new Except4SupportDocumented("ErrIdGenPosgr01", "Program error. Entity is already registered! " + sequenceName);
        if(sequencesNames.contains(sequenceName))
            throw new Except4SupportDocumented("ErrIdGenPosgr02", "Program error. Sequence is already registered! " + sequenceName);
        sequencesNames.add(sequenceName);
            // only for checking programmers ##
        sequences.put(entityName, new SequenceCache(entityName, sequenceName, step));
    }
    
   
        @Override
        public synchronized long getNextVal
        (String entityName) throws Except4Support {
            SequenceCache kSequence = sequences.get(entityName);
            try {
                return kSequence.nextVal();
            } catch (Exception e) {
            }

            List<Long> aRes;
            String sSql = "SELECT nextval('" + kSequence.getSequenceName() + "')";
            try {
                Query xQuery = entityManager.createNativeQuery(sSql, Long.class);
                aRes = xQuery.getResultList();
            } catch (Exception ex) {
                throw new Except4SupportDocumented("ErrIdGen01", "Error during execute: " + sSql, ex);
            }
            if (aRes == null || (aRes.size() < 1)) {
                throw new Except4SupportDocumented("ErrIdGen02", "Null result during execute: " + sSql);
            }
            return kSequence.setNewVal(aRes.get(0));
        }
    }

