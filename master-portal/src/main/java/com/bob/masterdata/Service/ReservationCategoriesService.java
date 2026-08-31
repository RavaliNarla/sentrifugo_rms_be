package com.bob.masterdata.Service;

import com.bob.commonutil.exception.CommonException;
import com.bob.commonutil.exception.ResourceNotFoundException;
import com.bob.commonutil.service.ExcelTemplateService;
import com.bob.db.dto.ReservationCategoriesDTO;
import com.bob.db.entity.ReservationCategoriesEntity;
import com.bob.db.enums.ReservationType;
import com.bob.db.mapper.ReservationCategoriesMapper;
import com.bob.db.repository.ReservationCategoriesRepository;
import com.bob.commonutil.util.AppConstants;
import com.bob.commonutil.exception.ExcelValidationException;
import com.bob.masterdata.utils.BulkValidationUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ReservationCategoriesService {

    @Autowired
    private ReservationCategoriesRepository reservationCategoriesRepository;

    @Autowired
    private ReservationCategoriesMapper reservationCategoriesMapper;

    @Autowired
    private ExcelTemplateService excelTemplateService;

    @PersistenceContext
    private EntityManager entityManager;

    public ReservationCategoriesDTO createReservationCategories(ReservationCategoriesDTO reservationCategoriesDto) {
        try {
            ReservationCategoriesEntity reservationCategoriesEntity= reservationCategoriesMapper.toEntity(reservationCategoriesDto);
            reservationCategoriesRepository.save(reservationCategoriesEntity);
            return reservationCategoriesMapper.toDto(reservationCategoriesEntity);
        } catch (Exception e) {
            return null;
        }
    }

    public List<ReservationCategoriesDTO> getAllResCategories(ReservationType reservationType) {
        try {

            List<ReservationCategoriesEntity> categories;

            if (reservationType != null) {
                categories = reservationCategoriesRepository
                        .findByReservationType(
                                reservationType,
                                Sort.by(Sort.Direction.ASC, AppConstants.MASTER_DISPLAY_ORDER)
                        );
            } else {
                categories = reservationCategoriesRepository
                        .findAll(
                                Sort.by(Sort.Direction.ASC, AppConstants.MASTER_DISPLAY_ORDER)
                        );
            }

            return reservationCategoriesMapper.toDtoList(categories);

        } catch (Exception e) {
            throw new CommonException("Failed to fetch Reservation categories");
        }
    }

    public ReservationCategoriesDTO updateResCategories(UUID id, ReservationCategoriesDTO reservationCategoriesDto) {
        ReservationCategoriesEntity reservationCategoriesEntity=reservationCategoriesRepository.findById(id).orElseThrow(()->new ResourceNotFoundException("Reservation Category not found"));
        reservationCategoriesMapper.updateEntityFromDto(reservationCategoriesDto,reservationCategoriesEntity);
        reservationCategoriesRepository.save(reservationCategoriesEntity);
        return reservationCategoriesMapper.toDto(reservationCategoriesEntity);
    }

    public ReservationCategoriesDTO deleteReservationCategory(UUID id) {
        ReservationCategoriesEntity reservationCategoriesEntity=reservationCategoriesRepository.findById(id).orElseThrow(()->new ResourceNotFoundException("Reservation Category not found"));
        ReservationCategoriesDTO reservationCategoriesDto =reservationCategoriesMapper.toDto(reservationCategoriesEntity);
        reservationCategoriesRepository.delete(reservationCategoriesEntity);
        return reservationCategoriesDto;
    }

    public List<ReservationCategoriesDTO> bulkSave(MultipartFile file) {
        try {
            List<ReservationCategoriesDTO> reservationCategoriesDTOS =
                    excelTemplateService.excelToDto(
                            file.getInputStream(),
                            ReservationCategoriesDTO.class
                    );

            List<String> errors = new ArrayList<>();

            errors.addAll(
                    BulkValidationUtils.collectDuplicateErrors(
                            reservationCategoriesDTOS,
                            ReservationCategoriesDTO::getCategoryName,
                            entityManager,
                            ReservationCategoriesEntity.class,
                            "categoryName",
                            "Reservation Category",
                            "Category Name"   // Excel column name
                    )
            );
            errors.addAll(
                    BulkValidationUtils.collectDuplicateErrors(
                    reservationCategoriesDTOS,
                    ReservationCategoriesDTO::getCategoryCode,
                    entityManager,
                    ReservationCategoriesEntity.class,
                    "categoryCode",
                    "Reservation Category",
                    "Category Code"   // Excel column name
            ));

            if (!errors.isEmpty()) {
                throw new ExcelValidationException(errors);
            }

            List<ReservationCategoriesEntity> entities =
                    reservationCategoriesMapper.toEntityList(reservationCategoriesDTOS);

            return reservationCategoriesMapper.toDtoList(
                    reservationCategoriesRepository.saveAll(entities)
            );

        } catch (IOException e) {
            throw new CommonException(
                    "Fail to store excel data");
        }
    }


}
