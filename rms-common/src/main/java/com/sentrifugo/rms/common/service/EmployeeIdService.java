package com.sentrifugo.rms.common.service;

import com.sentrifugo.rms.db.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmployeeIdService {

    private final UserRepository userRepository;

    /** Next code in EMP0001, EMP0002, … sequence. */
    public synchronized String nextEmployeeId() {
        Integer max = userRepository.findMaxEmployeeSequence();
        int next = (max == null ? 0 : max) + 1;
        return String.format("EMP%04d", next);
    }
}
