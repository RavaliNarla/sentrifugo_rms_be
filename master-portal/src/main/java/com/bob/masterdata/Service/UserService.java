package com.bob.masterdata.Service;

import com.bob.commonutil.exception.CommonException;
import com.bob.commonutil.exception.ExcelValidationException;
import com.bob.commonutil.exception.ManualValidationException;
import com.bob.commonutil.exception.ResourceNotFoundException;
import com.bob.commonutil.service.ExcelTemplateService;
import com.bob.db.dto.UserDTO;
import com.bob.db.entity.RoleEntity;
import com.bob.db.entity.UserEntity;
import com.bob.db.mapper.UserMapper;
import com.bob.db.repository.RoleRepository;
import com.bob.db.repository.UserRepository;
import com.bob.commonutil.util.AppConstants;
import com.bob.masterdata.Model.UserExcelModel;
import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ExcelTemplateService excelTemplateService;

    @Autowired
    private RoleRepository roleRepository;

    public UserDTO createUser(UserDTO userDTO){
        try {
            if(userRepository.existsByEmailIgnoreCase(userDTO.getEmail())){
                throw new CommonException("User already exists with this email");
            }
            UserEntity saved = userRepository.save(userMapper.toEntity(userDTO));
            return userMapper.toDTO(saved);
        } catch (Exception e) {
            throw new CommonException("Failed" + e.getMessage());
        }
    }

    public List<UserDTO> getAllUsers(){
        try {
            List<UserEntity> entities = userRepository.findAll(Sort.by(Sort.Direction.DESC,
                    AppConstants.MASTER_CREATED_DATE));
            return entities.stream().map(userMapper::toDTO).toList();
        } catch (Exception e) {
            throw new CommonException("Failed to fetch user");
        }
    }

    public UserDTO updateUser(UUID id, UserDTO userDTO) {
        UserEntity entity = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        userMapper.updateEntityFromDto(userDTO, entity);

        UserEntity saved = userRepository.save(entity);

        return userMapper.toDTO(saved);
    }

    public UserDTO deleteUser(UUID id) {
        try {
            Optional<UserEntity> entityOpt = userRepository.findById(id);
            if (entityOpt.isPresent()) {
                UserEntity entity = entityOpt.get();
//                entity.setIsActive(false);
//                userRepository.save(entity);
                userRepository.deleteById(id);
                return userMapper.toDTO(entity);
            } else {
                throw new ResourceNotFoundException("User not found.");
            }
        } catch (Exception e) {
            return null;
        }
    }

    @Transactional(readOnly = true)
    public Page<UserDTO> searchUsers(String search, Pageable pageable) {
        // Defense in depth: Validate search parameter (should be caught at controller level too)
        if (search == null || search.trim().isEmpty()) {
            throw new IllegalArgumentException("Search parameter is required and cannot be empty");
        }
        
        Specification<UserEntity> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Sanitize LIKE special characters to prevent LIKE injection attacks
            String sanitizedSearch = search.trim()
                    .replace("\\", "\\\\")
                    .replace("%", "\\%")
                    .replace("_", "\\_");
            String searchLower = "%" + sanitizedSearch.toLowerCase() + "%";
            Predicate namePredicate = criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), searchLower);
            Predicate emailPredicate = criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), searchLower);
            Predicate rolePredicate = criteriaBuilder.like(criteriaBuilder.lower(root.get("role")), searchLower);
            
            predicates.add(criteriaBuilder.or(namePredicate, emailPredicate, rolePredicate));

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        Page<UserEntity> entitiesPage = userRepository.findAll(spec, pageable);
        return entitiesPage.map(userMapper::toDTO);
    }

    public List<UserDTO> bulkSave(MultipartFile file) throws IOException {
        List<UserExcelModel> userExcelModels=excelTemplateService.excelToDto(file.getInputStream(), UserExcelModel.class);
        List<String> validations=validateUser(userExcelModels);
        if(!validations.isEmpty()){
            throw new ExcelValidationException(validations);
        }
        List<UserDTO> userDTOS=convertExcelModelToDTO(userExcelModels);
        List<String> emails=userDTOS.stream().map(UserDTO::getEmail).toList();
        Map<String, UserEntity> userMailToEntityMap =
                userRepository.findByEmailIgnoreCaseIn(emails)
                        .stream()
                        .collect(Collectors.toMap(
                                user -> user.getEmail().toLowerCase(),
                                user -> user
                        ));
        List<UserEntity> entitiesToSave=new ArrayList<>();
        for (UserDTO userDTO : userDTOS) {
            String emailKey = userDTO.getEmail().toLowerCase();
            if (userMailToEntityMap.containsKey(emailKey)) {
                //update if email already exists
                UserEntity existingEntity = userMailToEntityMap.get(emailKey);
                userMapper.updateEntityFromDto(userDTO, existingEntity);
                entitiesToSave.add(existingEntity);
            } else {
                UserEntity newEntity = userMapper.toEntity(userDTO);
                entitiesToSave.add(newEntity);
            }
        }
        //save all at once
        List<UserEntity> savedEntities = userRepository.saveAll(entitiesToSave);
        return userMapper.toDTOList(savedEntities);
    }
    //Create method for converting excelModel to dto
    public List<UserDTO> convertExcelModelToDTO(List<UserExcelModel> userExcelModels){
        List<UserDTO> userDTOS=new ArrayList<>();
        for(UserExcelModel userExcelModel : userExcelModels){
            UserDTO userDTO=new UserDTO();
            userDTO.setName(userExcelModel.getName());
            userDTO.setEmail(userExcelModel.getEmail());
            userDTO.setRole(userExcelModel.getRole());
            userDTO.setInterviewCenterId(userExcelModel.getInterviewCenterId());
            userDTOS.add(userDTO);
        }
        return userDTOS;
    }
    public List<String> validateUser(List<UserExcelModel> userDTOS){
        List<String> validations =new ArrayList<>();
        userDTOS.forEach((user)->{
            if(user.getRole().equals(AppConstants.ZONAL_HR_ROLE) && user.getInterviewCenterId()==null) {
                validations.add("Interview center is required for zonal hr role for email: " + user.getEmail());
            }
        });
        return validations;
    }
}
