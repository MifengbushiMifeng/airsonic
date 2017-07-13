/*
 This file is part of Libresonic.

 Libresonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Libresonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Libresonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2016 (C) Libresonic Authors
 Based upon Subsonic, Copyright 2009 (C) Sindre Mehus
 */
package org.libresonic.player.controller;

import org.apache.commons.lang.StringUtils;
import org.libresonic.player.domain.Transcoding;
import org.libresonic.player.service.SettingsService;
import org.libresonic.player.service.TranscodingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for the page used to administrate the set of transcoding configurations.
 *
 * @author Sindre Mehus
 */
@Controller
@RequestMapping("/transcodingSettings")
public class TranscodingSettingsController {

    @Autowired
    private TranscodingService transcodingService;
    @Autowired
    private SettingsService settingsService;

    @RequestMapping(method = RequestMethod.GET)
    public String doGet(Model model) throws Exception {

        Map<String, Object> map = new HashMap<String, Object>();

        map.put("transcodings", transcodingService.getAllTranscodings());
        map.put("transcodeDirectory", transcodingService.getTranscodeDirectory());
        map.put("downsampleCommand", settingsService.getDownsamplingCommand());
        map.put("hlsCommand", settingsService.getHlsCommand());
        map.put("brand", settingsService.getBrand());

        model.addAttribute("model", map);
        return "transcodingSettings";
    }

    @RequestMapping(method = RequestMethod.POST)
    public String doPost(HttpServletRequest request, RedirectAttributes redirectAttributes) throws Exception {
        String error = handleParameters(request, redirectAttributes);
        if(error != null) {
            redirectAttributes.addFlashAttribute("settings_toast", true);
        }
        redirectAttributes.addFlashAttribute("error", error);
        return "redirect:transcodingSettings.view";
    }

    private String handleParameters(HttpServletRequest request, RedirectAttributes redirectAttributes) {

        for (Transcoding transcoding : transcodingService.getAllTranscodings()) {
            Integer id = transcoding.getId();
            String name = getParameter(request, "name", id);
            String sourceFormats = getParameter(request, "sourceFormats", id);
            String targetFormat = getParameter(request, "targetFormat", id);
            String step1 = getParameter(request, "step1", id);
            String step2 = getParameter(request, "step2", id);
            boolean delete = getParameter(request, "delete", id) != null;

            if (delete) {
                transcodingService.deleteTranscoding(id);
            } else if (name == null) {
                return "transcodingsettings.noname";
            } else if (sourceFormats == null) {
                return "transcodingsettings.nosourceformat";
            } else if (targetFormat == null) {
                return "transcodingsettings.notargetformat";
            } else if (step1 == null) {
                return "transcodingsettings.nostep1";
            } else {
                transcoding.setName(name);
                transcoding.setSourceFormats(sourceFormats);
                transcoding.setTargetFormat(targetFormat);
                transcoding.setStep1(step1);
                transcoding.setStep2(step2);
                transcodingService.updateTranscoding(transcoding);
            }
        }

        String name = StringUtils.trimToNull(request.getParameter("name"));
        String sourceFormats = StringUtils.trimToNull(request.getParameter("sourceFormats"));
        String targetFormat = StringUtils.trimToNull(request.getParameter("targetFormat"));
        String step1 = StringUtils.trimToNull(request.getParameter("step1"));
        String step2 = StringUtils.trimToNull(request.getParameter("step2"));
        boolean defaultActive = request.getParameter("defaultActive") != null;

        if (name != null || sourceFormats != null || targetFormat != null || step1 != null || step2 != null) {
            Transcoding transcoding = new Transcoding(null, name, sourceFormats, targetFormat, step1, step2, null, defaultActive);
            String error = null;
            if (name == null) {
                error = "transcodingsettings.noname";
            } else if (sourceFormats == null) {
                error = "transcodingsettings.nosourceformat";
            } else if (targetFormat == null) {
                error = "transcodingsettings.notargetformat";
            } else if (step1 == null) {
                error = "transcodingsettings.nostep1";
            } else {
                transcodingService.createTranscoding(transcoding);
            }
            if(error != null) {
                redirectAttributes.addAttribute("newTranscoding", transcoding);
                return error;
            }
        }
        settingsService.setDownsamplingCommand(StringUtils.trim(request.getParameter("downsampleCommand")));
        settingsService.setHlsCommand(StringUtils.trim(request.getParameter("hlsCommand")));
        settingsService.save();
        return null;
    }

    private String getParameter(HttpServletRequest request, String name, Integer id) {
        return StringUtils.trimToNull(request.getParameter(name + "[" + id + "]"));
    }
}