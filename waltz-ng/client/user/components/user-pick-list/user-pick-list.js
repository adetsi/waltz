/*
 * Waltz - Enterprise Architecture
 * Copyright (C) 2016, 2017, 2018, 2019 Waltz open source project
 * See README.md for more information
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import {initialiseData} from "../../../common";
import template from "./user-pick-list.html";
import _ from "lodash";


const bindings = {
    people: "<",
    canRemoveLast: "<?",
    onRemove: "<",
    onAdd: "<",
    requiredRole: "@?"
};


const initialState = {
    requiredRole: null,
    canRemoveLast: false,
    addingPerson: false,
    onRemove: p => console.log("wupl: on-remove-person", p),
    onAdd: p => console.log("wupl: on-add-person", p)
};


function controller() {
    const vm = initialiseData(this, initialState);

    vm.personFilter = (p) => {
        return ! _.some(vm.people, existing => existing.id === p.id);
    };

    // -- INTERACT --


    vm.onStartAddPerson = () => {
        vm.addingPerson = true;
    };

    vm.onCancelAddPerson = () => {
        vm.addingPerson = false;
    };

    vm.onSelectPerson = (p) => vm.selectedPerson = p;

    vm.onAddPerson = () => {
        if (vm.selectedPerson) {
            vm.onAdd(vm.selectedPerson);
            vm.onCancelAddPerson();
        }
    }

}

controller.$inject = [];


const component = {
    template,
    controller,
    bindings
};


export default {
    component,
    id: "waltzUserPickList"
};