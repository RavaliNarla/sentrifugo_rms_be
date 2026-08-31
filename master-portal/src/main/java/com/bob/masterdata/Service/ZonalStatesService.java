package com.bob.masterdata.Service;

import com.bob.commonutil.util.AppConstants;
import com.bob.db.dto.ZonalStatesDTO;
import com.bob.db.mapper.ZonalStatesMapper;
import com.bob.db.repository.ZonalStatesRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ZonalStatesService {

    @Autowired
    private ZonalStatesRepository zonalStatesRepository;

    @Autowired
    private ZonalStatesMapper zonalStatesMapper;

    public List<ZonalStatesDTO> getAllZonalStates() {
        return zonalStatesMapper.toDtoList(zonalStatesRepository.findAll(Sort.by(Sort.Direction.ASC, AppConstants.MASTER_ZONAL_STATE_NAME)));
    }


}
