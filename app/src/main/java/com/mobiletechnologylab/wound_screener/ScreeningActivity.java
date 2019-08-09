package com.mobiletechnologylab.wound_screener;

import android.os.Bundle;
import android.widget.Button;

import com.mobiletechnologylab.apilib.apis.wound.diagnostics.add_measurement.wound_questionnaire.Choices.PATIENT_LOCATION_TYPES;
import com.mobiletechnologylab.apilib.apis.wound.diagnostics.add_measurement.wound_questionnaire.Choices.POD_TYPES;
import com.mobiletechnologylab.apilib.apis.wound.diagnostics.add_measurement.wound_questionnaire.PostRequest.WoundQuestionnaire;
import com.mobiletechnologylab.storagelib.questionnaire_utils.BaseQuestionnaireActivity;
import com.mobiletechnologylab.storagelib.questionnaire_utils.question_types.Question;
import com.mobiletechnologylab.storagelib.questionnaire_utils.question_types.SingleChoiceQuestion;
import com.mobiletechnologylab.wound_screener.ScreeningActivity.QUESTIONS_ID;
import java.util.ArrayList;
import java.util.LinkedHashMap;

public class ScreeningActivity extends BaseQuestionnaireActivity<QUESTIONS_ID> {

    private WoundQuestionnaire answers;
    Boolean kinyarwanda = true;

    public enum QUESTIONS_ID {
        PATIENT_LOCATION,
        POD,
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        answers = new WoundQuestionnaire();
    }

    @Override
    protected String getNoticeText() {
        return null;
    }

    @Override
    protected String getNoticeIntroText() {
        return null;
    }

    @Override
    protected LinkedHashMap<QUESTIONS_ID, Question> getQuestions() {
        LinkedHashMap<QUESTIONS_ID, Question> questions = new LinkedHashMap<>();

        if (kinyarwanda) {
            questions.put(QUESTIONS_ID.PATIENT_LOCATION, new SingleChoiceQuestion(
                    "Aho umurwayi ari (1 of 2)",
                    "Umurwayi ari ku bitaro cg ni mu rugo?",
                    new ArrayList<>(PATIENT_LOCATION_TYPES.choices().keySet()),
                    (i) -> answers.setPatientLocation(PATIENT_LOCATION_TYPES.values()[i].getCode())));

            questions.put(QUESTIONS_ID.POD, new SingleChoiceQuestion(
                    "Umunsi (2 of 2)",
                    "Ni uwuhe munsi?",
                    new ArrayList<>(POD_TYPES.choices().keySet()),
                    (i) -> answers.setPod(POD_TYPES.values()[i].getCode())));
            return questions;
        }
        else {
            questions.put(QUESTIONS_ID.PATIENT_LOCATION, new SingleChoiceQuestion(
                    "Patient Location (1 of 2)",
                    "Is the patient in the hospital or at home?",
                    new ArrayList<>(PATIENT_LOCATION_TYPES.choices().keySet()),
                    (i) -> answers.setPatientLocation(PATIENT_LOCATION_TYPES.values()[i].getCode())));

            questions.put(QUESTIONS_ID.POD, new SingleChoiceQuestion(
                    "POD (2 of 2)",
                    "What type of visit is this?",
                    new ArrayList<>(POD_TYPES.choices().keySet()),
                    (i) -> answers.setPod(POD_TYPES.values()[i].getCode())));
            return questions;
        }
    }

    @Override
    protected Object getAnswers() {
        return answers;
    }

    @Override
    protected String getQuestionnaireName() {
        return "Wound Questionnaire";
    }

}
