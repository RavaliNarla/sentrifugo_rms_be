package com.bob.masterdata.Service;

import com.bob.db.dto.UserSignatryDto;
import com.bob.db.mapper.UserSignatryMapper;
import com.bob.db.repository.UserSignatryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserSignatryService {

    @Autowired
    private UserSignatryRepository userSignatryRepository;

    @Autowired
    private UserSignatryMapper userSignatryMapper;

    public List<UserSignatryDto> getAllSignatries(){
        return userSignatryMapper.toDTOList(userSignatryRepository.findAll());
    }
}
