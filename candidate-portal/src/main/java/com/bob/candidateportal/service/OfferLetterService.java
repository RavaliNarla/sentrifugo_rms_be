package com.bob.candidateportal.service;

import com.bob.candidateportal.model.OfferLetterModel;
import com.bob.commonutil.exception.ResourceNotFoundException;
import com.bob.commonutil.service.AzureBlobStorageService;
import com.bob.db.entity.CandidateOffersEntity;
import com.bob.db.repository.CandidateOffersRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class OfferLetterService {

    @Autowired
    private AzureBlobStorageService azureBlobStorageService;

    @Autowired
    private CandidateOffersRepository candidateOffersRepository;


    public OfferLetterModel getMyOffers(UUID appId) {
        CandidateOffersEntity offer = candidateOffersRepository
                .findByCandidateApplication_Id(appId)
                .orElseThrow(() -> new ResourceNotFoundException("Offer not found"));

        OfferLetterModel offerLetterModel=OfferLetterModel.builder()
                        .offerLetterUrl(azureBlobStorageService.generateReadSasUrl(offer.getOfferFileUrl()))
                        .acceptBeforeDate(offer.getAcceptBeforeDate())
                .build();
        return offerLetterModel;
    }


/*

    @Autowired
    private CandidateDocumentsMapper mapper;

    public CandidateDocumentsDTO getOfferLetterDetails(UUID applicationId){
        CandidateDocumentsEntity entity =
                candidateDocumentsRepository
                        .findByDocumentTypeAndApplicationId(
                                AppConstants.OFFER_LETTER_TYPE,
                                applicationId
                        )
                        .orElseThrow(() ->
                                new RuntimeException("Offer letter not found"));

        return mapper.toDto(entity);
    }*/


}
