package com.bob.masterdata.Model;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class StateLanguageModel {
    private UUID stateId;
    private List<UUID> languageIds;
}
