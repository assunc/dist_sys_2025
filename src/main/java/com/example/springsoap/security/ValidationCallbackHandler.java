package com.example.springsoap.security;

import org.apache.wss4j.common.ext.WSPasswordCallback;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import com.example.springsoap.Repositories.BrokerRepository;
import com.example.springsoap.Entities.Broker;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import java.io.IOException;

@Component
public class ValidationCallbackHandler implements CallbackHandler {

    private final BrokerRepository brokerRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public ValidationCallbackHandler(BrokerRepository brokerRepository) {
        this.brokerRepository = brokerRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    @Override
    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
        for (Callback callback : callbacks) {
            if (callback instanceof WSPasswordCallback pc) {
                Broker user = brokerRepository.findByUsername(pc.getIdentifier());
                pc.setPassword(user.getHashedPassword());
            } else {
                throw new UnsupportedCallbackException(callback, "Unrecognized Callback");
            }
        }
    }
}
